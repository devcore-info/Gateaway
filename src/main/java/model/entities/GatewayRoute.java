package model.entities;

public class GatewayRoute {
    private String key;
    private String targetUrl;

    public GatewayRoute() {}

    public GatewayRoute(String key, String targetUrl) {
        this.key = key;
        this.targetUrl = targetUrl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}
