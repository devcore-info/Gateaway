package model.data;

import java.util.HashMap;
import java.util.Map;
import model.entities.GatewayRoute;

public class RouteConfig {
    private final Map<String, GatewayRoute> routes = new HashMap<>();

    public RouteConfig() {
        // Map clients and support routing keys to our Spring Boot backend service
        routes.put("clients", new GatewayRoute("clients", "http://localhost:8080/Backend/api/v1/clients"));
        routes.put("support", new GatewayRoute("support", "http://localhost:8080/Backend/api/v1/support"));
    }

    public GatewayRoute lookup(String key) {
        return routes.get(key);
    }
}
