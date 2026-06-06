# Gateaway - Middleware Proxy Service

Este proyecto actúa como la **Gateway (Middleware)** para el sistema de Mesa de Ayuda de la empresa **ConneXtion** (Desarrollo de Software III). Su función principal es servir como el punto único de entrada para las peticiones HTTP originadas en la aplicación cliente (frontend) y enrutarlas dinámicamente hacia el Backend de Spring Boot correspondiente, resolviendo de manera centralizada problemas de CORS y mapeo de rutas.

## 🚀 Características
- **CORS Centralizado:** Habilita el intercambio de recursos entre diferentes orígenes (Cross-Origin Resource Sharing) para permitir peticiones directas desde el navegador.
- **Proxy HTTP Dinámico:** Utiliza `java.net.http.HttpClient` para redirigir peticiones de tipo `GET`, `POST`, `PUT` y `DELETE` de manera transparente.
- **Mapeo de Rutas Modular:** Configurado en `model.data.RouteConfig` para mapear los recursos de `/clients/*` y `/support/*` a los microservicios correctos.
- **Empaquetado WAR:** Diseñado como un proyecto Web Maven para ser desplegado directamente en un servidor **Apache Tomcat** local o institucional.

## 🛠️ Tecnologías Utilizadas
- **Java 8**
- **Java Servlets (Jakarta EE 8 / javax.servlet)**
- **Maven**

## 📂 Estructura del Proyecto
- `controller/GatewayServlet.java`: Servlet principal que intercepta todas las peticiones bajo la ruta `/gateway/*`.
- `model/entities/GatewayRoute.java`: Representa un destino del proxy (ruta clave y URL base de destino).
- `model/data/RouteConfig.java`: Registra las rutas activas dirigidas a la API del Backend.

## 🧑‍💻 Ejecución y Despliegue
1. Abra el proyecto en **NetBeans**.
2. Realice un **Clean and Build** para generar el archivo `Gateaway.war` en la carpeta `target/`.
3. Despliegue el WAR en su instancia local de **Apache Tomcat** (por defecto en el puerto `8080`).
4. La Gateway estará expuesta y lista para recibir peticiones en:
   `http://localhost:8080/Gateaway/gateway/`
