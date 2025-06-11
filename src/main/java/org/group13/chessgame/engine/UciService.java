package org.group13.chessgame.engine;

import java.io.*;
import java.util.concurrent.CompletableFuture;

public class UciService {
    private final String enginePath;
    private Process engineProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private CompletableFuture<String> bestMoveFuture;
    private CompletableFuture<Boolean> startupFuture;

    public UciService(String enginePath) {
        this.enginePath = enginePath;
    }

    public CompletableFuture<Boolean> startEngine() {
        try {
            ProcessBuilder pb = new ProcessBuilder(enginePath);
            engineProcess = pb.start();

            reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));

            Thread listenerThread = new Thread(this::listenToEngine);
            listenerThread.setDaemon(true);
            listenerThread.start();

            this.startupFuture = new CompletableFuture<>();

            sendCommand("uci");
            return startupFuture;
        } catch (IOException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    private void listenToEngine() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("ENGINE >> " + line);
                if (line.equals("uciok")) {
                    sendCommand("isready");
                }
                if (line.equals("readyok")) {
                    if (startupFuture != null && !startupFuture.isDone()) {
                        startupFuture.complete(true);
                    }
                }
                if (line.startsWith("bestmove")) {
                    String bestMoveUci = line.split(" ")[1];
                    if (bestMoveFuture != null && !bestMoveFuture.isDone()) {
                        bestMoveFuture.complete(bestMoveUci);
                    }
                }
            }
        } catch (IOException e) {
            if (bestMoveFuture != null && !bestMoveFuture.isDone()) {
                bestMoveFuture.completeExceptionally(e);
            }
        }
    }

    public void sendCommand(String command) {
        try {
            System.out.println("GUI >> " + command);
            writer.write(command + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<String> findBestMove(String positionCommand, int moveTimeMillis) {
        bestMoveFuture = new CompletableFuture<>();
        sendCommand("ucinewgame");
        sendCommand(positionCommand);
        sendCommand("go movetime " + moveTimeMillis);
        return bestMoveFuture;
    }

    public void stopEngine() {
        if (engineProcess != null) {
            sendCommand("quit");
            engineProcess.destroy();
        }
    }
}
