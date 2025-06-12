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
import java.net.ServerSocket;
import java.net.Socket;

public class HostGameController {

    @FXML
    private TextField portField;

    @FXML
    private Text statusText;

    @FXML
    private Button startServerButton;

    @FXML
    private Button cancelButton;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private int port;
    private boolean serverStartedSuccessfully = false;

    @FXML
    private void handleStartServer(ActionEvent event) {
        String portStr = portField.getText();

        if (portStr.isEmpty()) {
            statusText.setText("Port cannot be empty.");
            return;
        }

        try {
            port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                statusText.setText("Port must be between 1024 and 65535.");
                return;
            }

            // Disable buttons while server is starting/waiting
            startServerButton.setDisable(true);
            cancelButton.setDisable(true);
            portField.setDisable(true);

            statusText.setText("Waiting for another player on port " + port + "...");
            System.out.println("Host: Waiting for another player on port " + port + "...");

            // Start server listening in a background thread
            Task<Void> serverTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        serverSocket = new ServerSocket(port);
                        clientSocket = serverSocket.accept(); // This blocks until a client connects

                        Platform.runLater(() -> {
                            statusText.setText("Player connected successfully!");
                            System.out.println("Host: Player connected successfully!");
                            serverStartedSuccessfully = true; // Mark success
                            // Close the dialog after successful connection (or keep open and signal ChessController)
                            // For this example, we close the dialog.
                            Stage stage = (Stage) startServerButton.getScene().getWindow();
                            stage.close();
                        });

                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            statusText.setText("Failed to start server or accept connection: " + e.getMessage());
                            System.err.println("Host Error: " + e.getMessage());
                            startServerButton.setDisable(false); // Re-enable buttons on error
                            cancelButton.setDisable(false);
                            portField.setDisable(false);
                            // Optionally, close the server socket on error
                            try {
                                if (serverSocket != null && !serverSocket.isClosed()) {
                                    serverSocket.close();
                                }
                            } catch (IOException closeEx) {
                                System.err.println("Error closing server socket: " + closeEx.getMessage());
                            }
                        });
                    } finally {
                        // Ensure server socket is closed if client socket is obtained or if an error occurs
                        // The server socket should be closed after accepting one client for a 1-to-1 game
                        try {
                            if (serverSocket != null && !serverSocket.isClosed()) {
                                serverSocket.close();
                                System.out.println("Host: ServerSocket closed.");
                            }
                        } catch (IOException e) {
                            System.err.println("Error closing server socket: " + e.getMessage());
                        }
                    }
                    return null;
                }
            };

            // Handle cancellation (if user closes dialog before connection)
            serverTask.setOnCancelled(e -> {
                System.out.println("Host: Server task cancelled.");
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } catch (IOException ex) {
                    System.err.println("Error closing server socket on cancel: " + ex.getMessage());
                }
            });

            new Thread(serverTask).start();

        } catch (NumberFormatException e) {
            statusText.setText("Invalid port number. Please enter a valid integer.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        System.out.println("Host: Cancel button clicked.");
        // If a server task is running, attempt to cancel it
        // This part would ideally be managed by a Task reference
        // For simplicity, we just close the dialog. The Task's onCancelled would handle cleanup.
        serverStartedSuccessfully = false; // Reset flag
        closeSockets(); // Ensure sockets are closed if dialog is manually closed
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    // Helper method to close sockets safely
    private void closeSockets() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Host: Client socket closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Host: Server socket closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    // --- Getters for ChessController ---
    public Socket getClientSocket() {
        return clientSocket;
    }

    public boolean isServerStartedSuccessfully() {
        return serverStartedSuccessfully;
    }

    // Called automatically by FXMLLoader if a controller implements Initializable
    // public void initialize() {
    //     // Any initialization logic for the controller goes here
    // }
}