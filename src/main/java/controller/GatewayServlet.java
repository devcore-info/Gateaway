package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
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
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"error\":\"Endpoint not found\"}");
            return;
        }

        // Parse pathInfo to extract routing key and remaining sub-path
        String[] parts = pathInfo.split("/", 3);
        if (parts.length < 2) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"error\":\"Invalid routing key\"}");
            return;
        }

        String routeKey = parts[1];
        String subPath = parts.length > 2 ? "/" + parts[2] : "";

        GatewayRoute targetRoute = routeConfig.lookup(routeKey);
        if (targetRoute == null) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"error\":\"No route registered for: " + routeKey + "\"}");
            return;
        }

        // Construct target URL
        String targetUrl = targetRoute.getTargetUrl() + subPath;
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request.getMethod());
            
            // Forward request headers from client to backend
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!headerName.equalsIgnoreCase("host")) {
                    conn.setRequestProperty(headerName, request.getHeader(headerName));
                }
            }

            // Forward request body if method is POST, PUT, or DELETE
            String method = request.getMethod();
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                conn.setDoOutput(true);
                try (InputStream is = request.getInputStream();
                     OutputStream os = conn.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            }

            // Read response code from backend
            int responseCode = conn.getResponseCode();
            response.setStatus(responseCode);

            // Forward response headers back to client (skip CORS headers since we set them manually)
            for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                String key = entry.getKey();
                if (key != null && !key.equalsIgnoreCase("transfer-encoding") &&
                    !key.equalsIgnoreCase("access-control-allow-origin") &&
                    !key.equalsIgnoreCase("access-control-allow-credentials")) {
                    for (String value : entry.getValue()) {
                        response.addHeader(key, value);
                    }
                }
            }

            // Forward response body back to client
            InputStream is = null;
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                is = conn.getErrorStream();
            }

            if (is != null) {
                response.setContentType("application/json;charset=UTF-8");
                try (OutputStream os = response.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            }

        } catch (Exception ex) {
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"error\":\"Gateway routing error: " + ex.getMessage() + "\"}");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
