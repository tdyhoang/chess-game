package org.group13.chessgame.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class JoinGameController {

    @FXML
    private TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private Text statusText;

    @FXML
    private Button connectButton;

    @FXML
    private Button cancelButton;

    private Socket clientSocket;
    private String ipAddress;
    private int port;
    private boolean connectedSuccessfully = false;

    // This method is called when the "Connect" button is clicked
    @FXML
    private void handleConnect(ActionEvent event) {
        ipAddress = ipField.getText();
        String portStr = portField.getText();

        if (ipAddress.isEmpty() || portStr.isEmpty()) {
            statusText.setText("IP Address and Port cannot be empty.");
            return;
        }

        try {
            port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                statusText.setText("Port must be between 1024 and 65535.");
                return;
            }

            // Disable buttons while connecting
            connectButton.setDisable(true);
            cancelButton.setDisable(true);
            ipField.setDisable(true);
            portField.setDisable(true);

            statusText.setText("Attempting to connect to " + ipAddress + ":" + port + "...");
            System.out.println("Client: Attempting to connect to " + ipAddress + ":" + port + "...");

            // Attempt connection in a background thread
            Task<Void> connectTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        clientSocket = new Socket(ipAddress, port); // This blocks until connected or fails

                        Platform.runLater(() -> {
                            statusText.setText("Connected successfully to host!");
                            System.out.println("Client: Connected successfully to host!");
                            connectedSuccessfully = true; // Mark success
                            Stage stage = (Stage) connectButton.getScene().getWindow();
                            stage.close(); // Close dialog on success
                        });

                    } catch (ConnectException e) {
                        Platform.runLater(() -> {
                            statusText.setText("Connection refused. Is the host server running?");
                            System.err.println("Client Error: Connection refused - " + e.getMessage());
                            connectedSuccessfully = false; // Mark failure
                        });
                    } catch (UnknownHostException e) {
                        Platform.runLater(() -> {
                            statusText.setText("Unknown host. Check the IP address.");
                            System.err.println("Client Error: Unknown host - " + e.getMessage());
                            connectedSuccessfully = false;
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            statusText.setText("Connection error: " + e.getMessage() + ". Wrong IP or port?");
                            System.err.println("Client Error: IO Exception - " + e.getMessage());
                            connectedSuccessfully = false;
                        });
                    } finally {
                        // Re-enable buttons if connection failed in background
                        Platform.runLater(() -> {
                            if (!connectedSuccessfully) { // Only re-enable if not closing due to success
                                connectButton.setDisable(false);
                                cancelButton.setDisable(false);
                                ipField.setDisable(false);
                                portField.setDisable(false);
                            }
                        });
                    }
                    return null;
                }
            };

            // Handle cancellation if user closes dialog before connection
            connectTask.setOnCancelled(e -> {
                System.out.println("Client: Connect task cancelled.");
                closeSocket();
            });

            new Thread(connectTask).start();

        } catch (NumberFormatException e) {
            statusText.setText("Invalid port number. Please enter a valid integer.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        System.out.println("Client: Cancel button clicked.");
        connectedSuccessfully = false; // Reset flag
        closeSocket(); // Ensure socket is closed if dialog is manually closed
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    // Helper method to close the socket safely
    private void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Client: Socket closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }

    // --- Getters for ChessController ---
    public Socket getClientSocket() {
        return clientSocket;
    }

    public boolean isConnectedSuccessfully() {
        return connectedSuccessfully;
    }
}