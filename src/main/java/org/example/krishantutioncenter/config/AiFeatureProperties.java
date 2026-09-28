package org.example.krishantutioncenter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiFeatureProperties {

    private String apiKey = "";
    private String model = "gemini-2.5-flash-lite";
    private int monthlyRequestLimit = 100;
    private int timeoutSeconds = 25;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMonthlyRequestLimit() {
        return monthlyRequestLimit;
    }

    public void setMonthlyRequestLimit(int monthlyRequestLimit) {
        this.monthlyRequestLimit = monthlyRequestLimit;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !model.isBlank()
                && monthlyRequestLimit > 0 && timeoutSeconds > 0;
    }
}
