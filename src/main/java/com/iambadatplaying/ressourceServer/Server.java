package com.iambadatplaying.ressourceServer;

import com.iambadatplaying.Starter;
import com.iambadatplaying.frontendHandler.LocalWebSocket;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Server {

    private final Starter                         starter;
    private final ArrayList<Pattern>               allowedOrigins;
    private final Pattern                         localHostPattern;
    private       org.eclipse.jetty.server.Server server;
    private       ProxyHandler                    proxyHandler;

    private final List<LocalWebSocket> localWebSockets = Collections.synchronizedList(new ArrayList<>());

    public Server(Starter starter) {
        this.starter = starter;
        allowedOrigins = new ArrayList<>();
        if (Starter.isDev) {
            localHostPattern = Pattern.compile("^(http://)?(localhost|127\\.0\\.0\\.1):(" + Starter.RESOURCE_SERVER_PORT + "|" + Starter.DEBUG_FRONTEND_PORT + "|" + Starter.DEBUG_FRONTEND_PORT_V2 + ")(/)?$");
        } else {
            localHostPattern = Pattern.compile("^(http://)?(localhost|127\\.0\\.0\\.1):" + Starter.RESOURCE_SERVER_PORT + "(/)?$");
        }
        addAllowedOrigins();
    }

    private void addAllowedOrigins() {
        //Allows certain origins to access the resource server.
        //For example, if an external website wants to access the resource server, it must be added here.
    }

    public void init() {
        server = new org.eclipse.jetty.server.Server(Starter.RESOURCE_SERVER_PORT);

        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
        connector.setPort(Starter.RESOURCE_SERVER_PORT);

        int maxHeaderSize = 1_024 * 8;
        HttpConfiguration httpConfig = connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        httpConfig.setRequestHeaderSize(maxHeaderSize);

        proxyHandler = new ProxyHandler(starter);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase(Starter.class.getResource("/html").toExternalForm());

        ResourceHandler userDataHandler = new ResourceHandler();
        userDataHandler.setDirectoriesListed(true);
        userDataHandler.setResourceBase(starter.getConfigLoader().getAppFolderPath().toAbsolutePath().toString());

        RESTContextHandler restContext = new RESTContextHandler(starter);
        restContext.setContextPath("/rest");

        ServletContextHandler wsContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        wsContext.setContextPath("/ws");

        WebSocketHandler websocketHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.setCreator((req, resp) -> {
                    if (Starter.getInstance().getServer().filterWebSocketRequest(req)) {
                        return null;
                    }

                    return new LocalWebSocket(starter);
                });
            }
        };
        wsContext.insertHandler(websocketHandler);

        // Proxy-prefix to handle Proxy requests
        ContextHandler proxyContext = new ContextHandler();
        proxyContext.setContextPath("/proxy");
        proxyContext.setHandler(proxyHandler);

        // Static-prefix for static resources
        ContextHandler staticContext = new ContextHandler() {

            @Override
            public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                DispatcherType dispatch = baseRequest.getDispatcherType();
                boolean newContext = baseRequest.takeNewContext();
                try {
                    if (newContext) {
                        this.requestInitialized(baseRequest, request);
                    }

                    if (filterRequest(request, response)) {
                        baseRequest.setHandled(true);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    if (dispatch == DispatcherType.REQUEST && this.isProtectedTarget(target)) {
                        baseRequest.setHandled(true);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    this.nextHandle(target, baseRequest, request, response);
                } finally {
                    if (newContext) {
                        this.requestDestroyed(baseRequest, request);
                    }

                }

            }
        };
        staticContext.setContextPath("/static");
        staticContext.setHandler(resourceHandler);

        ContextHandler userDataContext = new ContextHandler() {
            @Override
            public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                DispatcherType dispatch = baseRequest.getDispatcherType();
                boolean newContext = baseRequest.takeNewContext();
                try {
                    if (newContext) {
                        this.requestInitialized(baseRequest, request);
                    }

                    if (filterRequest(request, response)) {
                        baseRequest.setHandled(true);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    if (dispatch == DispatcherType.REQUEST && this.isProtectedTarget(target)) {
                        baseRequest.setHandled(true);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    this.nextHandle(target, baseRequest, request, response);
                } finally {
                    if (newContext) {
                        this.requestDestroyed(baseRequest, request);
                    }

                }
            }
        };
        userDataContext.setContextPath("/config");
        userDataContext.setHandler(userDataHandler);

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(proxyContext);
        handlerList.addHandler(wsContext);
        handlerList.addHandler(userDataContext);
        handlerList.addHandler(staticContext);
        handlerList.addHandler(restContext);

        server.setHandler(handlerList);

        try {
            server.start();
        } catch (Exception e) {
            starter.log("Error starting resource server: " + e.getMessage(), Starter.LOG_LEVEL.ERROR);
            starter.exit(Starter.EXIT_CODE.SERVER_BIND_FAILED);
        }
    }

    public void resetCachedData() {
        proxyHandler.resetCache();
    }

    public boolean filterWebSocketRequest(ServletUpgradeRequest req) {
        String origin = req.getHeader("Origin");

        //Either local host OR non-browser request
        return filterOrigin(origin);
    }

    public boolean filterRequest(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");

        //Either local host OR non-browser request
        if (!filterOrigin(origin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type");

            return false;
        }

        return true;
    }

    public boolean filterOrigin(String origin) {
        if (origin == null) {
            return false;
        }


        return !localHostPattern.matcher(origin).matches() && allowedOrigins.stream().noneMatch(pattern -> pattern.matcher(origin).matches());
    }

    public List<LocalWebSocket> getLocalWebSockets() {
        return localWebSockets;
    }

    public void removeSocket(LocalWebSocket localWebSocket) {
        localWebSockets.remove(localWebSocket);
    }

    public void addSocket(LocalWebSocket localWebSocket) {
        localWebSockets.add(localWebSocket);
    }

    public void sendToAllSessions(String message) {
        for (LocalWebSocket localWebSocket : localWebSockets) {
            localWebSocket.sendMessage(message);
        }
    }

    public void shutdown() {
        for (LocalWebSocket localWebSocket : localWebSockets) {
            if (localWebSocket == null) continue;
            localWebSocket.externalShutdown();
        }
        localWebSockets.clear();
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        server = null;
    }
}
