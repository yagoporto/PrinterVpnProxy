package com.printervpnp.server;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import com.printervpnp.connection.ConnectionHandler;
import com.printervpnp.manager.ConfigManager;
import com.printervpnp.log.Logger;
import com.printervpnp.reloadHTTP.HttpConfigServer;

public class PrinterProxyServer {
    private ServerSocket serverSocket;
    private ConfigManager configManager;
    private HttpConfigServer httpServer;

    public PrinterProxyServer(String configFilePath) {
        configManager = new ConfigManager(configFilePath);

        try {
            serverSocket = new ServerSocket(configManager.getVpnPort());
            Logger.log("Servidor iniciado na porta " + configManager.getVpnPort());

            // Inicia o servidor HTTP na porta 9000
            httpServer = new HttpConfigServer(9000, configManager);
        } catch (IOException e) {
            Logger.log("Erro ao iniciar o servidor: " + e.getMessage());
        }

        startConsoleListener();
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                Logger.log("Conexão recebida de " + clientIp);

                ConfigManager.PrinterInfo printerInfo = configManager.getPrinterInfo(clientIp);

                if (printerInfo != null) {
                    Logger.log("Encaminhando tráfego de " + clientIp + " para " + printerInfo.getLocalIp());
                    new Thread(new ConnectionHandler(clientSocket, printerInfo.getLocalIp(), printerInfo.getLocalPort())).start();
                } else {
                    Logger.log("Nenhuma impressora encontrada para o IP VPN: " + clientIp);
                    clientSocket.close();
                }

            } catch (IOException e) {
                Logger.log("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    private void startConsoleListener() {
        Thread consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String command = scanner.nextLine().trim();
                if (command.equalsIgnoreCase("r")) {
                    Logger.log("Comando recebido: recarregando configurações...");
                    configManager.reloadConfig();
                }
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.start();
    }
}