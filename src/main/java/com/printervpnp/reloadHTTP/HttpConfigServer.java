package com.printervpnp.reloadHTTP;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.printervpnp.manager.ConfigManager;
import com.printervpnp.log.Logger;

public class HttpConfigServer {
    private HttpServer server;
    private ConfigManager configManager;

    public HttpConfigServer(int port, ConfigManager configManager) throws IOException {
        this.configManager = configManager;
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Endpoint para recarregar configuração
        server.createContext("/reload", new ReloadHandler());

        server.setExecutor(null);
        server.start();
        Logger.log("Servidor HTTP iniciado na porta " + port);
    }

    class ReloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Logger.log("Requisição recebida para recarregar configurações...");
                configManager.reloadConfig();
                String response = "Configuração recarregada com sucesso!";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Método não permitido
            }
        }
    }
}
