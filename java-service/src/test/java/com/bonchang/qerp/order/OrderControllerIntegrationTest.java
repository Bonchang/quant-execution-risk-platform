package com.bonchang.qerp.order;

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
                .andExpect(jsonPath("$.clientOrderId").value("client-001"));

        Integer fillCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fill",
                Integer.class
        );
        Integer positionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM position",
                Integer.class
        );

        org.assertj.core.api.Assertions.assertThat(fillCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(positionCount).isEqualTo(1);
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
        jdbcTemplate.update(
                """
                INSERT INTO market_price(instrument_id, price_date, open_price, high_price, low_price, close_price, volume)
                VALUES (?, CURRENT_DATE, 100.000000, 110.000000, 95.000000, 105.000000, 1000)
                """,
                instrumentId
        );
    }
}
