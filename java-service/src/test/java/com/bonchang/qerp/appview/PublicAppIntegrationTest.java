package com.bonchang.qerp.appview;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonchang.qerp.appauth.GuestAuthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PublicAppIntegrationTest {

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
        registry.add("market-data.execution-slippage-bps", () -> "0");
        registry.add("market-data.stale-threshold-seconds", () -> "3600");
        registry.add("market-data.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM outbox_event");
        jdbcTemplate.execute("DELETE FROM cash_ledger_entry");
        jdbcTemplate.execute("DELETE FROM risk_check_result");
        jdbcTemplate.execute("DELETE FROM portfolio_snapshot");
        jdbcTemplate.execute("DELETE FROM fill");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM position");
        jdbcTemplate.execute("DELETE FROM strategy_run");
        jdbcTemplate.execute("DELETE FROM app_user");
        jdbcTemplate.execute("DELETE FROM cash_balance");
        jdbcTemplate.execute("DELETE FROM account");
        jdbcTemplate.update("UPDATE market_quote SET received_at = NOW(), quote_time = NOW(), source = 'PUBLIC_DEMO_SEED'");
    }

    @Test
    void issueGuestSession_provisionsDedicatedAccountAndMeEndpoint() throws Exception {
        GuestAuthResponse session = issueGuestSession();

        mockMvc.perform(authorizedGet("/app/me", session.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(session.userId()))
                .andExpect(jsonPath("$.authType").value("GUEST"))
                .andExpect(jsonPath("$.role").value("ROLE_GUEST"))
                .andExpect(jsonPath("$.displayName").value(session.displayName()))
                .andExpect(jsonPath("$.account.accountId").value(session.accountId()))
                .andExpect(jsonPath("$.account.accountCode").value(session.accountCode()))
                .andExpect(jsonPath("$.account.availableCash").value(100000.0));
    }

    @Test
    void appHome_withoutSession_returnsGuestEmptyState() throws Exception {
        mockMvc.perform(get("/app/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestAvailable").value(true))
                .andExpect(jsonPath("$.assetSummary.accountId").doesNotExist())
                .andExpect(jsonPath("$.assetSummary.ownerName").value("게스트 세션을 시작해 주세요"))
                .andExpect(jsonPath("$.featuredStocks").isArray());
    }

    @Test
    void guestOrdersAreScopedPerAccount() throws Exception {
        GuestAuthResponse guestA = issueGuestSession();
        GuestAuthResponse guestB = issueGuestSession();

        Long orderId = createConsumerOrder(guestA.accessToken(), Map.of(
                "symbol", "AAPL",
                "side", "BUY",
                "quantity", "1.000000",
                "orderType", "MARKET",
                "timeInForce", "DAY"
        ));

        mockMvc.perform(authorizedGet("/app/orders", guestA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalOrders").value(1))
                .andExpect(jsonPath("$.orders[0].id").value(orderId))
                .andExpect(jsonPath("$.orders[0].symbol").value("AAPL"));

        mockMvc.perform(authorizedGet("/app/orders/" + orderId, guestA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.accountId").value(guestA.accountId()))
                .andExpect(jsonPath("$.instrumentSymbol").value("AAPL"));

        mockMvc.perform(authorizedGet("/app/portfolio", guestA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.accountId").value(guestA.accountId()))
                .andExpect(jsonPath("$.recentExecutions[0].orderId").value(orderId));

        mockMvc.perform(authorizedGet("/app/orders", guestB.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalOrders").value(0))
                .andExpect(jsonPath("$.orders").isEmpty());

        mockMvc.perform(authorizedGet("/app/orders/" + orderId, guestB.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void staleQuoteRejectsMarketOrderForGuestSession() throws Exception {
        GuestAuthResponse session = issueGuestSession();
        Long instrumentId = jdbcTemplate.queryForObject(
                "SELECT id FROM instrument WHERE symbol = 'AAPL'",
                Long.class
        );
        jdbcTemplate.update(
                "UPDATE market_quote SET received_at = ?, quote_time = ? WHERE instrument_id = ?",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(2),
                instrumentId
        );

        mockMvc.perform(authorizedPost("/app/orders", session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "symbol", "AAPL",
                                "side", "BUY",
                                "quantity", "1.000000",
                                "orderType", "MARKET",
                                "timeInForce", "DAY"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("live quote required for market orders"));
    }

    private GuestAuthResponse issueGuestSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/app/auth/guest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_GUEST"))
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), GuestAuthResponse.class);
    }

    private Long createConsumerOrder(String accessToken, Map<String, String> payload) throws Exception {
        MvcResult result = mockMvc.perform(authorizedPost("/app/orders", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private MockHttpServletRequestBuilder authorizedGet(String url, String accessToken) {
        return get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private MockHttpServletRequestBuilder authorizedPost(String url, String accessToken) {
        return post(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }
}
