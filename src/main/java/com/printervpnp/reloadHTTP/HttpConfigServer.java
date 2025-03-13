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
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // 📌 Lendo o corpo da requisição corretamente
                String body = new String(exchange.getRequestBody().readAllBytes());
                Logger.log("Corpo recebido: " + body);
    
                // 🔹 Convertendo parâmetros para um Map
                Map<String, String> params = parseFormData(body);
                
                // 🔍 Debug: Exibir parâmetros extraídos
                Logger.log("Parâmetros extraídos: " + params);
    
                String vpnIp = params.get("vpnIp");
                String localIp = params.get("localIp");
                String portStr = params.get("port");
    
                if (vpnIp == null || localIp == null || portStr == null) {
                    String response = "Dados inválidos! Certifique-se de enviar vpnIp, localIp e port.";
                    exchange.sendResponseHeaders(400, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }
    
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    String response = "Porta inválida!";
                    exchange.sendResponseHeaders(400, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }
    
                // Adiciona impressora ao ConfigManager
                configManager.addPrinter(vpnIp, localIp, port);
    
                String response = "Impressora adicionada com sucesso!";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Método não permitido
            }
        }
    
        // Método para converter dados do Form-Encode para um Map
        private Map<String, String> parseFormData(String formData) {
            return Arrays.stream(formData.split("&"))
                    .map(param -> param.split("="))
                    .filter(pair -> pair.length == 2)
                    .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
        }
    }
}