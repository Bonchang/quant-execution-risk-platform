package com.bonchang.qerp.portfolio;

import com.bonchang.qerp.fill.Fill;
import com.bonchang.qerp.instrument.Instrument;
import com.bonchang.qerp.order.Order;
import com.bonchang.qerp.order.OrderSide;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealizedPnlCalculatorTest {

    private final RealizedPnlCalculator calculator = new RealizedPnlCalculator();

    @Test
    void calculate_buyOnly_returnsZero() {
        List<Fill> fills = List.of(
                fill(1L, OrderSide.BUY, "10.000000", "100.000000"),
                fill(1L, OrderSide.BUY, "5.000000", "110.000000")
        );

        org.assertj.core.api.Assertions.assertThat(calculator.calculate(fills))
                .isEqualByComparingTo("0.000000");
    }

    @Test
    void calculate_buyThenSell_usesAverageCostBeforeSell() {
        List<Fill> fills = List.of(
                fill(1L, OrderSide.BUY, "10.000000", "100.000000"),
                fill(1L, OrderSide.SELL, "4.000000", "120.000000")
        );

        org.assertj.core.api.Assertions.assertThat(calculator.calculate(fills))
                .isEqualByComparingTo("80.000000");
    }

    @Test
    void calculate_multipleBuyThenSell_preservesWeightedAverageBehavior() {
        List<Fill> fills = List.of(
                fill(1L, OrderSide.BUY, "10.000000", "100.000000"),
                fill(1L, OrderSide.BUY, "10.000000", "120.000000"),
                fill(1L, OrderSide.SELL, "5.000000", "130.000000")
        );

        org.assertj.core.api.Assertions.assertThat(calculator.calculate(fills))
                .isEqualByComparingTo("100.000000");
    }

    @Test
    void calculate_partialSellSequence_accumulatesRealizedPnlConsistently() {
        List<Fill> fills = List.of(
                fill(1L, OrderSide.BUY, "10.000000", "100.000000"),
                fill(1L, OrderSide.SELL, "3.000000", "110.000000"),
                fill(1L, OrderSide.SELL, "2.000000", "90.000000")
        );

        org.assertj.core.api.Assertions.assertThat(calculator.calculate(fills))
                .isEqualByComparingTo("10.000000");
    }

    @Test
    void calculate_sellBeyondLongQuantity_doesNotOvercountRealizedPnl() {
        List<Fill> fills = List.of(
                fill(1L, OrderSide.BUY, "2.000000", "100.000000"),
                fill(1L, OrderSide.SELL, "5.000000", "130.000000")
        );

        org.assertj.core.api.Assertions.assertThat(calculator.calculate(fills))
                .isEqualByComparingTo("60.000000");
    }

    private Fill fill(Long instrumentId, OrderSide side, String quantity, String price) {
        Instrument instrument = new Instrument();
        instrument.setId(instrumentId);

        Order order = new Order();
        order.setSide(side);

        Fill fill = new Fill();
        fill.setInstrument(instrument);
        fill.setOrder(order);
        fill.setFillQuantity(new BigDecimal(quantity));
        fill.setFillPrice(new BigDecimal(price));
        fill.setFilledAt(LocalDateTime.now());
        return fill;
    }
}
