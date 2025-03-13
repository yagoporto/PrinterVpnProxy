package com.printervpnp.reloadHTTP;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.printervpnp.manager.ConfigManager;
import com.printervpnp.log.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpConfigServer {
    private HttpServer server;
    private ConfigManager configManager;

    public HttpConfigServer(int port, ConfigManager configManager) throws IOException {
        this.configManager = configManager;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Endpoint para recarregar configurações
        server.createContext("/reload", new ReloadHandler());
        
        // Novo endpoint para exibir as impressoras cadastradas
        server.createContext("/printers", new PrinterHandler(configManager));

        server.setExecutor(null);  // Usando o executor padrão
        server.start();
        Logger.log("Servidor HTTP de configuração iniciado na porta " + port);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    // Método para analisar parâmetros de consulta
    private Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }
        return Arrays.stream(query.split("&"))
                .map(param -> param.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> pair[0], 
                        pair -> pair.length > 1 ? pair[1] : ""));
    }

    // Handler para o endpoint /reload
    class ReloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Logger.log("Requisição recebida no /reload");

                // Obtendo parâmetros da URL corretamente
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                String token = params.getOrDefault("token", "");

                // Pegando o token esperado e evitando NullPointerException
                String expectedToken = configManager.getAdminToken();
                expectedToken = (expectedToken != null) ? expectedToken.trim() : "";

                Logger.log("Token recebido: [" + token + "]");
                Logger.log("Token esperado: [" + expectedToken + "]");

                if (token.equals(expectedToken)) {
                    Logger.log("Token válido! Recarregando configurações...");
                    configManager.reloadConfig();
                    sendResponse(exchange, 200, "Configurações recarregadas com sucesso!");
                } else {
                    Logger.log("Acesso negado! Token inválido.");
                    sendResponse(exchange, 403, "Acesso negado!");
                }
            } else {
                sendResponse(exchange, 405, "Método não permitido");
            }
        }
    }

    // Handler para o endpoint /printers
    class PrinterHandler implements HttpHandler {
        private ConfigManager configManager;
    
        public PrinterHandler(ConfigManager configManager) {
            this.configManager = configManager;
        }
    
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
    
            if (method.equals("GET")) {
                handleGetPrinters(exchange);
            } else if (method.equals("POST")) {
                handlePostPrinter(exchange);
            } else if (method.equals("PUT")) {
                handlePutPrinter(exchange);
            } else if (method.equals("DELETE")) {
                handleDeletePrinter(exchange);
            } else {
                sendResponse(exchange, 405, "Método não permitido");
            }
        }
    
        private void handleGetPrinters(HttpExchange exchange) throws IOException {
            String response = configManager.getAllPrinters();
            sendResponse(exchange, 200, response);
        }
    
        private void handlePostPrinter(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes());
            Map<String, String> params = parseFormData(body);
    
            String vpnIp = params.get("vpnIp");
            String localIp = params.get("localIp");
            String port = params.get("port");
    
            if (vpnIp != null && localIp != null && port != null) {
                configManager.addPrinter(vpnIp, localIp, Integer.parseInt(port));
                sendResponse(exchange, 201, "Impressora adicionada com sucesso!");
            } else {
                sendResponse(exchange, 400, "Dados inválidos");
            }
        }
    
        private void handlePutPrinter(HttpExchange exchange) throws IOException {
            String vpnIp = exchange.getRequestURI().getPath().split("/")[2];
            String body = new String(exchange.getRequestBody().readAllBytes());
            Map<String, String> params = parseFormData(body);
    
            if (configManager.updatePrinter(vpnIp, params.get("localIp"), Integer.parseInt(params.get("port")))) {
                sendResponse(exchange, 200, "Impressora atualizada com sucesso!");
            } else {
                sendResponse(exchange, 404, "Impressora não encontrada!");
            }
        }
    
        private void handleDeletePrinter(HttpExchange exchange) throws IOException {
            String vpnIp = exchange.getRequestURI().getPath().split("/")[2];
    
            if (configManager.removePrinter(vpnIp)) {
                sendResponse(exchange, 200, "Impressora removida com sucesso!");
            } else {
                sendResponse(exchange, 404, "Impressora não encontrada!");
            }
        }
    
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    
        private Map<String, String> parseFormData(String body) {
            return body.lines()
                    .map(line -> line.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
        }
    }
}