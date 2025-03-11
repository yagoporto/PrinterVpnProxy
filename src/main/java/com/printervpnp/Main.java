package com.printervpnp;

import java.io.IOException;

import com.printervpnp.server.PrinterProxyServer;
import com.printervpnp.manager.ConfigManager;

public class Main {
    public static void main(String[] args) throws IOException {
        PrinterProxyServer proxyServer = new PrinterProxyServer("resources/config.properties");
        proxyServer.start();
        
        ConfigManager configManager = new ConfigManager("resources/config.properties");
        configManager.reloadConfig();
    }
}