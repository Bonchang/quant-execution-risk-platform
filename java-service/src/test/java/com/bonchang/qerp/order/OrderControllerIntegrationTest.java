package com.bonchang.qerp.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OrderControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("qerp")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM risk_check_result");
        jdbcTemplate.execute("DELETE FROM portfolio_snapshot");
        jdbcTemplate.execute("DELETE FROM fill");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM position");
        jdbcTemplate.execute("DELETE FROM market_price");
        jdbcTemplate.execute("DELETE FROM strategy_run");
        jdbcTemplate.execute("DELETE FROM instrument");
    }

    @Test
    void createOrder_success() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId);

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "MARKET",
                "clientOrderId", "client-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.strategyRunId").value(strategyRunId))
                .andExpect(jsonPath("$.instrumentId").value(instrumentId))
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.filledQuantity").value(10.0))
                .andExpect(jsonPath("$.remainingQuantity").value(0.0))
                .andExpect(jsonPath("$.clientOrderId").value("client-001"));

        Integer fillCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fill",
                Integer.class
        );
        Integer positionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM position",
                Integer.class
        );
        Integer snapshotCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM portfolio_snapshot",
                Integer.class
        );

        org.assertj.core.api.Assertions.assertThat(fillCount).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(positionCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(snapshotCount).isEqualTo(1);
    }

    @Test
    void createOrder_marketOrder_persistsMultipleFillsAndUpdatesPositionCumulatively() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId);

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "11.000000",
                "orderType", "MARKET",
                "clientOrderId", "market-multi-fill-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.filledQuantity").value(11.0))
                .andExpect(jsonPath("$.remainingQuantity").value(0.0));

        Integer fillCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fill WHERE order_id = (SELECT id FROM orders WHERE client_order_id = 'market-multi-fill-001')",
                Integer.class
        );
        org.assertj.core.api.Assertions.assertThat(fillCount).isEqualTo(2);

        String netQuantity = jdbcTemplate.queryForObject(
                "SELECT TO_CHAR(net_quantity, 'FM9999999990.000000') FROM position WHERE strategy_run_id = ? AND instrument_id = ?",
                String.class,
                strategyRunId,
                instrumentId
        );
        org.assertj.core.api.Assertions.assertThat(netQuantity).isEqualTo("11.000000");
    }

    @Test
    void createOrder_duplicateClientOrderIdPerStrategyRun_rejected() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId);

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "MARKET",
                "clientOrderId", "dup-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void createOrder_overLimit_rejectedAndNoFill() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId);

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "1500.000000",
                "orderType", "MARKET",
                "clientOrderId", "risk-reject-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        Integer fillCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fill", Integer.class);
        org.assertj.core.api.Assertions.assertThat(fillCount).isZero();
    }

    @Test
    void createOrder_limitBuy_fillsWhenMarketPriceAtOrBelowLimit() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "105.000000");

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "LIMIT",
                "limitPrice", "105.000000",
                "clientOrderId", "limit-partial-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.limitPrice").value(105.0))
                .andExpect(jsonPath("$.filledQuantity").value(10.0))
                .andExpect(jsonPath("$.remainingQuantity").value(0.0));

        Integer fillCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fill WHERE order_id = (SELECT id FROM orders WHERE client_order_id = 'limit-partial-001')",
                Integer.class
        );
        org.assertj.core.api.Assertions.assertThat(fillCount).isEqualTo(1);
    }

    @Test
    void createOrder_limitBuy_staysApprovedWhenMarketPriceAboveLimit() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "110.000000");

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "LIMIT",
                "limitPrice", "100.000000",
                "clientOrderId", "limit-buy-open-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.filledQuantity").value(0))
                .andExpect(jsonPath("$.remainingQuantity").value(10.0));

        Integer fillCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fill", Integer.class);
        Integer positionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM position", Integer.class);
        org.assertj.core.api.Assertions.assertThat(fillCount).isZero();
        org.assertj.core.api.Assertions.assertThat(positionCount).isZero();
    }

    @Test
    void createOrder_limitSell_fillsWhenMarketPriceAtOrAboveLimit() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "105.000000");

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "SELL",
                "quantity", "7.000000",
                "orderType", "LIMIT",
                "limitPrice", "100.000000",
                "clientOrderId", "limit-sell-fill-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.filledQuantity").value(7.0))
                .andExpect(jsonPath("$.remainingQuantity").value(0.0));
    }

    @Test
    void createOrder_limitSell_staysApprovedWhenMarketPriceBelowLimit() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "95.000000");

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "SELL",
                "quantity", "7.000000",
                "orderType", "LIMIT",
                "limitPrice", "100.000000",
                "clientOrderId", "limit-sell-open-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.filledQuantity").value(0))
                .andExpect(jsonPath("$.remainingQuantity").value(7.0));
    }

    @Test
    void createOrder_limitOrder_withoutLimitPrice_badRequest() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "105.000000");

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "LIMIT",
                "clientOrderId", "limit-invalid-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limitPrice is required for LIMIT order"));
    }

    @Test
    void createOrder_marketOrder_withLimitPrice_badRequest() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "105.000000");

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "MARKET",
                "limitPrice", "100.000000",
                "clientOrderId", "market-invalid-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limitPrice must be null for MARKET order"));
    }

    @Test
    void createOrder_missingStrategyRun_badRequest() throws Exception {
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId);

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", 999999L,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "MARKET",
                "clientOrderId", "bad-strategy-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("strategyRunId not found"));
    }

    @Test
    void ingestMarketData_withoutApiKey_returnsAcceptedWithFailureMessage() throws Exception {
        mockMvc.perform(post("/market-data/ingest"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.totalInstruments").value(0))
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andExpect(jsonPath("$.updatedSymbols").isArray())
                .andExpect(jsonPath("$.failures[0]").value("market-data.api-key is not configured"));
    }

    @Test
    void marketDataStatus_afterIngest_exposesLastResult() throws Exception {
        mockMvc.perform(post("/market-data/ingest"))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/market-data/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.apiKeyConfigured").value(false))
                .andExpect(jsonPath("$.lastResult.failureCount").value(0))
                .andExpect(jsonPath("$.lastResult.failures[0]").value("market-data.api-key is not configured"));
    }

    @Test
    void dashboardOverview_includesLatestPortfolioSummaryAndSnapshots() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "105.000000");

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "MARKET",
                "clientOrderId", "portfolio-overview-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/dashboard/overview?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioSummary.strategyRunId").value(strategyRunId))
                .andExpect(jsonPath("$.portfolioSummary.totalMarketValue").value(1050.0))
                .andExpect(jsonPath("$.portfolioSummary.realizedPnl").value(0.0))
                .andExpect(jsonPath("$.portfolioSummary.unrealizedPnl").value(0.0))
                .andExpect(jsonPath("$.recentPortfolioSnapshots[0].strategyRunId").value(strategyRunId));
    }

    @Test
    void refreshPortfolioSnapshots_usesLatestMarketPriceForUnrealizedPnl() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "105.000000");

        String payload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "MARKET",
                "clientOrderId", "portfolio-refresh-001"
        ));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        insertMarketPriceTomorrow(instrumentId, "120.000000");

        mockMvc.perform(post("/dashboard/portfolio-snapshots/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyRunCount").value(1));

        mockMvc.perform(get("/dashboard/overview?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioSummary.totalMarketValue").value(1200.0))
                .andExpect(jsonPath("$.portfolioSummary.realizedPnl").value(0.0))
                .andExpect(jsonPath("$.portfolioSummary.unrealizedPnl").value(150.0))
                .andExpect(jsonPath("$.portfolioSummary.totalPnl").value(150.0));
    }

    @Test
    void createOrder_sellFill_generatesRealizedPnlByAverageCostBeforeSell() throws Exception {
        Long strategyRunId = insertStrategyRun();
        Long instrumentId = insertInstrument();
        insertMarketPrice(instrumentId, "100.000000");

        String buyPayload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "BUY",
                "quantity", "10.000000",
                "orderType", "MARKET",
                "clientOrderId", "realized-buy-001"
        ));
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));

        insertMarketPriceTomorrow(instrumentId, "120.000000");

        String sellPayload = objectMapper.writeValueAsString(Map.of(
                "strategyRunId", strategyRunId,
                "instrumentId", instrumentId,
                "side", "SELL",
                "quantity", "4.000000",
                "orderType", "LIMIT",
                "limitPrice", "110.000000",
                "clientOrderId", "realized-sell-001"
        ));
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sellPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));

        mockMvc.perform(get("/dashboard/overview?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioSummary.realizedPnl").value(80.0))
                .andExpect(jsonPath("$.portfolioSummary.unrealizedPnl").value(120.0))
                .andExpect(jsonPath("$.portfolioSummary.totalPnl").value(200.0))
                .andExpect(jsonPath("$.portfolioSummary.totalMarketValue").value(720.0));
    }

    private Long insertStrategyRun() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO strategy_run(strategy_name, run_at, parameters_json)
                VALUES ('mvp-strategy', NOW(), '{}')
                RETURNING id
                """,
                Long.class
        );
    }

    private Long insertInstrument() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO instrument(symbol, name, market)
                VALUES ('AAPL', 'Apple Inc.', 'NASDAQ')
                RETURNING id
                """,
                Long.class
        );
    }

    private void insertMarketPrice(Long instrumentId) {
        insertMarketPrice(instrumentId, "105.000000");
    }

    private void insertMarketPrice(Long instrumentId, String closePrice) {
        jdbcTemplate.update(
                """
                INSERT INTO market_price(instrument_id, price_date, open_price, high_price, low_price, close_price, volume)
                VALUES (?, CURRENT_DATE, 100.000000, 110.000000, 95.000000, ?, 1000)
                """,
                instrumentId,
                new java.math.BigDecimal(closePrice)
        );
    }

    private void insertMarketPriceTomorrow(Long instrumentId, String closePrice) {
        jdbcTemplate.update(
                """
                INSERT INTO market_price(instrument_id, price_date, open_price, high_price, low_price, close_price, volume)
                VALUES (?, CURRENT_DATE + 1, 100.000000, 125.000000, 95.000000, ?, 1000)
                """,
                instrumentId,
                new java.math.BigDecimal(closePrice)
        );
    }
}
