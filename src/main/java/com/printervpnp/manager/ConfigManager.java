package com.printervpnp.manager;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

import com.printervpnp.log.Logger;

public class ConfigManager {
    private Properties properties;
    private Map<String, PrinterInfo> printerMap;
    private int vpnPort;
    private String configFilePath;

    

    public ConfigManager(String configFilePath) {
        this.configFilePath = configFilePath;
        Logger.log("📂 Carregando configuração de: " + configFilePath);
        loadConfig();
    }

    public void loadConfig() {
        properties = new Properties();
        printerMap = new HashMap<>();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                Logger.log("❌ Erro: Arquivo config.properties não encontrado no classpath!");
                return;
            }

            properties.load(input);
            vpnPort = Integer.parseInt(properties.getProperty("vpn.port", "9100"));

            // 🔹 Debug: Exibir todas as propriedades carregadas
            for (String key : properties.stringPropertyNames()) {
                Logger.log("🔍 Config: " + key + " = " + properties.getProperty(key));
            }

            int index = 1;
            while (properties.containsKey("printer." + index + ".vpn")) {
                String vpnIp = properties.getProperty("printer." + index + ".vpn");
                String localIp = properties.getProperty("printer." + index + ".local");
                int localPort = Integer.parseInt(properties.getProperty("printer." + index + ".port", "9100"));

                printerMap.put(vpnIp, new PrinterInfo(localIp, localPort, properties));
                index++;
            }

            Logger.log("✅ Configurações carregadas com sucesso!");

        } catch (IOException e) {
            Logger.log("❌ Erro ao carregar configurações: " + e.getMessage());
        }
    }

    public String getAllPrinters() {
        StringBuilder builder = new StringBuilder();
        printerMap.forEach((vpnIp, info) -> builder.append(vpnIp)
            .append(" -> ")
            .append(info.getLocalIp())
            .append(":")
            .append(info.getLocalPort())
            .append("\n"));
        return builder.toString();
    }

    public void addPrinter(String vpnIp, String localIp, int localPort) {
        printerMap.put(vpnIp, new PrinterInfo(localIp, localPort, properties));
        Logger.log("Nova impressora adicionada: " + vpnIp + " -> " + localIp);
    }

    public boolean updatePrinter(String vpnIp, String localIp, int localPort) {
        if (printerMap.containsKey(vpnIp)) {
            printerMap.put(vpnIp, new PrinterInfo(localIp, localPort, properties));
            Logger.log("Impressora atualizada: " + vpnIp + " -> " + localIp);
            return true;
        }
        return false;
    }

    public boolean removePrinter(String vpnIp) {
        if (printerMap.containsKey(vpnIp)) {
            printerMap.remove(vpnIp);
            Logger.log("Impressora removida: " + vpnIp);
            return true;
        }
        return false;
    }

    

    public void reloadConfig() {
        Logger.log("Recarregando configurações...");
        loadConfig();
    }

    public int getVpnPort() {
        return vpnPort;
    }

    public PrinterInfo getPrinterInfo(String vpnIp) {
        return printerMap.get(vpnIp);
    }

    public String getAdminToken() {
        String token = properties.getProperty("admin.token");
        if (token == null || token.trim().isEmpty()) {
            Logger.log("Aviso: Token de administrador não encontrado no config.properties!");
            return "DEFAULT_TOKEN"; // Valor padrão para evitar null
        }
        return token;
    }

    public static class PrinterInfo {
        private String localIp;
        private int localPort;

        private Properties properties;

        public PrinterInfo(String localIp, int localPort, Properties properties) {
            this.localIp = localIp;
            this.localPort = localPort;
            this.properties = properties;
        }

        public String getLocalIp() {
            return localIp;
        }

        public int getLocalPort() {
            return localPort;
        }

    }
}