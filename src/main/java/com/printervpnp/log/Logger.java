package com.printervpnp.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final String LOG_DIR = "logs"; // Pasta do log
    private static final String LOG_FILE = LOG_DIR + "/proxy.log";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void log(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = "[" + timestamp + "] " + message;

        System.out.println(logMessage); // Exibe no console

        try {
            // Criar diretório caso não exista
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Escrever no arquivo de log
            try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
                writer.write(logMessage + "\n");
            }
        } catch (IOException e) {
            System.err.println("Erro ao escrever no log: " + e.getMessage());
        }
    }
}
