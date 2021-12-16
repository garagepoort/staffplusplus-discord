package be.garagepoort.staffplusplus.discord.common.config;

public class WebhookConfig {

    private final String host;
    private final String apiKey;

    public WebhookConfig(String host, String apiKey) {
        this.host = host;
        this.apiKey = apiKey;
    }

    public String getHost() {
        return host;
    }

    public String getApiKey() {
        return apiKey;
    }
}
