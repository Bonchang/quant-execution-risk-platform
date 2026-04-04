package com.bonchang.qerp.marketdata;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market-data")
public class MarketDataProperties {

    private boolean enabled = false;
    private String baseUrl = "https://finnhub.io/api/v1";
    private String apiKey = "";
    private long pollMs = 60000;
    private long staleThresholdSeconds = 30;
    private int maxInstrumentsPerRun = 100;
    private String sourceMode = "FINNHUB_REST";
    private String bidAskFallbackRule = "MID_SPREAD";
    private BigDecimal syntheticSpreadBps = new BigDecimal("10.0");
    private BigDecimal executionSlippageBps = new BigDecimal("2.0");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getPollMs() {
        return pollMs;
    }

    public void setPollMs(long pollMs) {
        this.pollMs = pollMs;
    }

    public long getStaleThresholdSeconds() {
        return staleThresholdSeconds;
    }

    public void setStaleThresholdSeconds(long staleThresholdSeconds) {
        this.staleThresholdSeconds = staleThresholdSeconds;
    }

    public int getMaxInstrumentsPerRun() {
        return maxInstrumentsPerRun;
    }

    public void setMaxInstrumentsPerRun(int maxInstrumentsPerRun) {
        this.maxInstrumentsPerRun = maxInstrumentsPerRun;
    }

    public String getSourceMode() {
        return sourceMode;
    }

    public void setSourceMode(String sourceMode) {
        this.sourceMode = sourceMode;
    }

    public String getBidAskFallbackRule() {
        return bidAskFallbackRule;
    }

    public void setBidAskFallbackRule(String bidAskFallbackRule) {
        this.bidAskFallbackRule = bidAskFallbackRule;
    }

    public BigDecimal getSyntheticSpreadBps() {
        return syntheticSpreadBps;
    }

    public void setSyntheticSpreadBps(BigDecimal syntheticSpreadBps) {
        this.syntheticSpreadBps = syntheticSpreadBps;
    }

    public BigDecimal getExecutionSlippageBps() {
        return executionSlippageBps;
    }

    public void setExecutionSlippageBps(BigDecimal executionSlippageBps) {
        this.executionSlippageBps = executionSlippageBps;
    }
}
