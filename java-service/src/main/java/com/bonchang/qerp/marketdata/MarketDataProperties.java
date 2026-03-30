package com.bonchang.qerp.marketdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market-data")
public class MarketDataProperties {

    private boolean enabled = false;
    private String baseUrl = "https://finnhub.io/api/v1";
    private String apiKey = "";
    private long pollMs = 60000;
    private int maxInstrumentsPerRun = 100;

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

    public int getMaxInstrumentsPerRun() {
        return maxInstrumentsPerRun;
    }

    public void setMaxInstrumentsPerRun(int maxInstrumentsPerRun) {
        this.maxInstrumentsPerRun = maxInstrumentsPerRun;
    }
}
