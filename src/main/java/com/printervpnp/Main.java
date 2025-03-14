package com.printervpnp;

import java.io.IOException;

import com.printervpnp.server.PrinterProxyServer;
import com.printervpnp.manager.ConfigManager;
import com.printervpnp.log.Logger;
import com.printervpnp.*;

public class Main {
    public static void main(String[] args) throws IOException {

        String configFilePath = "./resources/config.properties";

        PrinterProxyServer proxyServer = new PrinterProxyServer(configFilePath);
        proxyServer.start();
        
        ConfigManager configManager = new ConfigManager(configFilePath);
        configManager.reloadConfig();
    }
}