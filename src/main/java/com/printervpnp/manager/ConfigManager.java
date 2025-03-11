package com.printervpnp.manager;


import java.io.FileInputStream;
import java.io.IOException;
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
        loadConfig();
    }

    public void loadConfig() {
        properties = new Properties();
        printerMap = new HashMap<>();

        try (FileInputStream input = new FileInputStream(configFilePath)) {
            properties.load(input);
            vpnPort = Integer.parseInt(properties.getProperty("vpn.port", "9100"));

            int index = 1;
            while (properties.containsKey("printer." + index + ".vpn")) {
                String vpnIp = properties.getProperty("printer." + index + ".vpn");
                String localIp = properties.getProperty("printer." + index + ".local");
                int localPort = Integer.parseInt(properties.getProperty("printer." + index + ".port", "9100"));

                printerMap.put(vpnIp, new PrinterInfo(localIp, localPort));
                index++;
            }

            Logger.log("Configurações recarregadas!");

        } catch (IOException e) {
            Logger.log("Erro ao carregar configurações: " + e.getMessage());
        }
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

    public static class PrinterInfo {
        private String localIp;
        private int localPort;

        public PrinterInfo(String localIp, int localPort) {
            this.localIp = localIp;
            this.localPort = localPort;
        }

        public String getLocalIp() {
            return localIp;
        }

        public int getLocalPort() {
            return localPort;
        }
    }
}