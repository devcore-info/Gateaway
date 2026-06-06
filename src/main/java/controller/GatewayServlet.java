package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.data.RouteConfig;
import model.entities.GatewayRoute;

@WebServlet(name = "GatewayServlet", urlPatterns = {"/gateway/*"})
public class GatewayServlet extends HttpServlet {

    private final RouteConfig routeConfig = new RouteConfig();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        setCorsHeaders(response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"Endpoint not found\"}");
            return;
        }

        // Parse pathInfo to extract routing key and remaining sub-path
        // e.g., "/clients/register" -> parts = ["", "clients", "register"]
        String[] parts = pathInfo.split("/", 3);
        if (parts.length < 2) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"Invalid routing key\"}");
            return;
        }

        String routeKey = parts[1];
        String subPath = parts.length > 2 ? "/" + parts[2] : "";

        GatewayRoute targetRoute = routeConfig.lookup(routeKey);
        if (targetRoute == null) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"No route registered for: " + routeKey + "\"}");
            return;
        }

        // Construct target URL
        String targetUrl = targetRoute.getTargetUrl() + subPath;
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        try {
            HttpRequest.Builder proxyReqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl));

            if ("GET".equalsIgnoreCase(request.getMethod())) {
                proxyReqBuilder.GET();
            } else if ("POST".equalsIgnoreCase(request.getMethod())) {
                StringBuilder buffer = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                }
                proxyReqBuilder.POST(HttpRequest.BodyPublishers.ofString(buffer.toString()))
                        .header("Content-Type", "application/json");
            } else if ("PUT".equalsIgnoreCase(request.getMethod())) {
                StringBuilder buffer = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                }
                proxyReqBuilder.PUT(HttpRequest.BodyPublishers.ofString(buffer.toString()))
                        .header("Content-Type", "application/json");
            } else if ("DELETE".equalsIgnoreCase(request.getMethod())) {
                proxyReqBuilder.DELETE();
            }

            // Execute request
            HttpResponse<String> proxyResponse = httpClient.send(
                    proxyReqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            // Write response back to client
            response.setStatus(proxyResponse.statusCode());
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(proxyResponse.body());

        } catch (Exception ex) {
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"error\":\"Gateway routing error: " + ex.getMessage() + "\"}");
        }
    }
}
