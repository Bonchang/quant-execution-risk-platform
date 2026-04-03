package com.bonchang.qerp.portfolio;

import com.bonchang.qerp.fill.Fill;
import com.bonchang.qerp.fill.FillRepository;
import com.bonchang.qerp.market.MarketPrice;
import com.bonchang.qerp.market.MarketPriceRepository;
import com.bonchang.qerp.order.OrderSide;
import com.bonchang.qerp.position.Position;
import com.bonchang.qerp.position.PositionRepository;
import com.bonchang.qerp.strategyrun.StrategyRun;
import com.bonchang.qerp.strategyrun.StrategyRunRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioSnapshotService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final PositionRepository positionRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final StrategyRunRepository strategyRunRepository;
    private final FillRepository fillRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public PortfolioSnapshot createSnapshotForStrategyRun(Long strategyRunId) {
        List<Position> positions = positionRepository.findByStrategyRunId(strategyRunId);

        BigDecimal totalMarketValue = ZERO;
        BigDecimal unrealizedPnl = ZERO;
        BigDecimal totalCostBasis = ZERO;

        for (Position position : positions) {
            BigDecimal quantity = toScale(position.getNetQuantity());
            BigDecimal averagePrice = toScale(position.getAveragePrice());
            BigDecimal marketPrice = resolveReferencePrice(position.getInstrument().getId()).orElse(averagePrice);

            BigDecimal marketValue = quantity.multiply(marketPrice).setScale(6, RoundingMode.HALF_UP);
            BigDecimal costBasis = quantity.multiply(averagePrice).setScale(6, RoundingMode.HALF_UP);
            BigDecimal pnl = quantity.multiply(marketPrice.subtract(averagePrice)).setScale(6, RoundingMode.HALF_UP);

            totalMarketValue = totalMarketValue.add(marketValue).setScale(6, RoundingMode.HALF_UP);
            totalCostBasis = totalCostBasis.add(costBasis).setScale(6, RoundingMode.HALF_UP);
            unrealizedPnl = unrealizedPnl.add(pnl).setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal realizedPnl = calculateRealizedPnl(strategyRunId);
        BigDecimal totalPnl = unrealizedPnl.add(realizedPnl).setScale(6, RoundingMode.HALF_UP);
        BigDecimal returnRate = ZERO;
        if (totalCostBasis.compareTo(BigDecimal.ZERO) != 0) {
            returnRate = totalPnl.divide(totalCostBasis, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100.000000"))
                    .setScale(6, RoundingMode.HALF_UP);
        }

        PortfolioSnapshot snapshot = new PortfolioSnapshot();
        snapshot.setStrategyRun(entityManager.getReference(StrategyRun.class, strategyRunId));
        snapshot.setSnapshotAt(LocalDateTime.now());
        snapshot.setTotalMarketValue(totalMarketValue);
        snapshot.setUnrealizedPnl(unrealizedPnl);
        snapshot.setRealizedPnl(realizedPnl);
        snapshot.setTotalPnl(totalPnl);
        snapshot.setReturnRate(returnRate);
        return portfolioSnapshotRepository.save(snapshot);
    }

    @Transactional
    public int refreshSnapshotsForAllStrategyRuns() {
        List<StrategyRun> strategyRuns = strategyRunRepository.findAll();
        for (StrategyRun strategyRun : strategyRuns) {
            createSnapshotForStrategyRun(strategyRun.getId());
        }
        return strategyRuns.size();
    }

    private Optional<BigDecimal> resolveReferencePrice(Long instrumentId) {
        return marketPriceRepository.findFirstByInstrumentIdOrderByPriceDateDescIdDesc(instrumentId)
                .map(MarketPrice::getClosePrice)
                .map(this::toScale);
    }

    private BigDecimal toScale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * MVP 실현손익 규칙:
     * SELL fill 시점의 평균단가(매도 직전) 기준으로
     * qty * (sellPrice - avgCostBeforeSell)를 누적한다.
     */
    private BigDecimal calculateRealizedPnl(Long strategyRunId) {
        List<Fill> fills = fillRepository.findByStrategyRunIdOrderByFilledAtAscIdAsc(strategyRunId);
        Map<Long, PositionCostState> states = new HashMap<>();
        BigDecimal realizedPnl = ZERO;

        for (Fill fill : fills) {
            Long instrumentId = fill.getInstrument().getId();
            PositionCostState state = states.computeIfAbsent(instrumentId, id -> new PositionCostState());
            BigDecimal quantity = toScale(fill.getFillQuantity());
            BigDecimal price = toScale(fill.getFillPrice());

            if (fill.getOrder().getSide() == OrderSide.BUY) {
                BigDecimal newQuantity = state.netQuantity.add(quantity).setScale(6, RoundingMode.HALF_UP);
                BigDecimal weightedCost = state.netQuantity.multiply(state.averagePrice)
                        .add(quantity.multiply(price))
                        .setScale(6, RoundingMode.HALF_UP);
                state.netQuantity = newQuantity;
                state.averagePrice = newQuantity.compareTo(BigDecimal.ZERO) > 0
                        ? weightedCost.divide(newQuantity, 6, RoundingMode.HALF_UP)
                        : ZERO;
                continue;
            }

            BigDecimal averageBeforeSell = state.averagePrice;
            BigDecimal matchedQuantity = quantity.min(state.netQuantity.max(BigDecimal.ZERO)).setScale(6, RoundingMode.HALF_UP);
            BigDecimal contribution = matchedQuantity.multiply(price.subtract(averageBeforeSell))
                    .setScale(6, RoundingMode.HALF_UP);
            realizedPnl = realizedPnl.add(contribution).setScale(6, RoundingMode.HALF_UP);

            BigDecimal newQuantity = state.netQuantity.subtract(quantity).setScale(6, RoundingMode.HALF_UP);
            state.netQuantity = newQuantity;
            if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                state.averagePrice = ZERO;
            }
        }

        return realizedPnl;
    }

    private static class PositionCostState {
        private BigDecimal netQuantity = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        private BigDecimal averagePrice = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    }
}
