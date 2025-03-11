package com.printervpnp.connection;


import java.io.*;
import java.net.*;

import com.printervpnp.log.Logger;

public class ConnectionHandler implements Runnable {
    private Socket clientSocket;
    private String printerIp;
    private int printerPort;

    public ConnectionHandler(Socket clientSocket, String printerIp, int printerPort) {
        this.clientSocket = clientSocket;
        this.printerIp = printerIp;
        this.printerPort = printerPort;
    }

    @Override
    public void run() {
        try (Socket printerSocket = new Socket(printerIp, printerPort);
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
            InputStream printerInput = printerSocket.getInputStream();
            OutputStream printerOutput = printerSocket.getOutputStream()) {

            Logger.log("Encaminhando conexão para impressora em " + printerIp + ":" + printerPort);

            Thread clientToPrinter = new Thread(() -> forwardData(clientInput, printerOutput));
            Thread printerToClient = new Thread(() -> forwardData(printerInput, clientOutput));

            clientToPrinter.start();
            printerToClient.start();

            clientToPrinter.join();
            printerToClient.join();

        } catch (IOException | InterruptedException e) {
            Logger.log("Erro ao encaminhar conexão para " + printerIp + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Logger.log("Erro ao fechar conexão com o cliente: " + e.getMessage());
            }
        }
    }

    private void forwardData(InputStream input, OutputStream output) {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (IOException e) {
            Logger.log("Erro ao transferir dados: " + e.getMessage());
        }
    }
}
