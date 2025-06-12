package org.group13.chessgame.controller;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.group13.chessgame.engine.UciService;
import org.group13.chessgame.model.*;
import org.group13.chessgame.pgn.PgnHeaders;
import org.group13.chessgame.utils.PgnFormatter;
import org.group13.chessgame.utils.PgnParseException;
import org.group13.chessgame.utils.PgnParser;
import org.group13.chessgame.utils.PieceImageProvider;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChessController {
    private static final int BOARD_DISPLAY_SIZE = 600;
    private static final int SQUARE_SIZE = 75;
    private static final int BACKGROUND_RECT_INDEX = 0;
    private static final int HOVER_OVERLAY_INDEX = 1;
    private static final int SELECTED_OVERLAY_INDEX = 2;
    private static final int DRAG_OVERLAY_INDEX = 3;
    private static final int PIECE_IMAGE_VIEW_INDEX = 4;
    private static final int MOVE_INDICATOR_INDEX = 5;
    // --- Network Communication Codes (Re-introducing for clarity) ---
    private static final int NETWORK_MOVE_CODE = 0;
    private static final int NETWORK_SURRENDER_WHITE_CODE = 1;
    private static final int NETWORK_SURRENDER_BLACK_CODE = 2;
    private static final int NETWORK_CHAT_CODE = 3;
    private final List<Piece> whiteCapturedPieces = new ArrayList<>();
    private final List<Piece> blackCapturedPieces = new ArrayList<>();
    private final ObservableList<MovePairDisplay> moveHistoryObservableList = FXCollections.observableArrayList();
    private final ObservableList<String> chatHistoryObservableList = FXCollections.observableArrayList();
    private int currentPlyPointer = -1;
    @FXML
    private BorderPane rootPane;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
    @FXML
    private MenuItem surrenderMenuItem;
    @FXML
    private MenuItem offerDrawMenuItem;
    @FXML
    private HBox blackPlayerArea;
    @FXML
    private Label blackPlayerNameLabel;
    @FXML
    private FlowPane capturedByBlackArea;
    @FXML
    private HBox whitePlayerArea;
    @FXML
    private Label whitePlayerNameLabel;
    @FXML
    private FlowPane capturedByWhiteArea;
    @FXML
    private GridPane boardGridPane;
    @FXML
    private Label turnLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<MovePairDisplay> moveHistoryListView;
    @FXML
    private Button undoMoveButton;
    @FXML
    private Button redoMoveButton;
    @FXML
    private Button autoFlipBoardButton;
    @FXML
    private Button offerDrawButton;
    @FXML
    private Button surrenderButton;
    @FXML
    private TextField whitePlayerField;
    @FXML
    private TextField blackPlayerField;
    @FXML
    private TextField eventField;
    @FXML
    private TextField siteField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label resultLabel;
    @FXML
    private Tab chatHistoryTab;
    @FXML
    private ListView<String> chatHistoryListView;
    @FXML
    private TextField chatInputField;
    @FXML
    private TabPane infoTabPane;

    private Game gameModel;
    private UciService uciService;
    private StackPane[][] squarePanes;

    private Square selectedSquare = null;
    private List<Move> availableMovesForSelectedPiece = new ArrayList<>();
    private boolean boardIsFlipped = false;

    private GameMode currentMode = GameMode.ANALYSIS;
    private PieceColor playerColor = PieceColor.WHITE;
    private Difficulty currentDifficulty = Difficulty.MEDIUM;

    private MediaPlayer moveSoundPlayer, captureSoundPlayer, checkSoundPlayer, endGameSoundPlayer, castleSoundPlayer, promoteSoundPlayer;

    private boolean isAutoFlippingBoard = false;

    // Network related variables
    private PieceColor myColor;
    private PieceColor hostColor;
    private boolean isLanGameActive = false;
    private Socket gameSocket; // This will be the socket for the actual game communication
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Task<Void> networkListenerTask; // Task to listen for incoming moves
    private boolean isPlayingPvp = false;

    private void handleHostGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/group13/chessgame/host-game-view.fxml"));
            Parent hostGameRoot = loader.load();
            HostGameController hostGameController = loader.getController(); // Get controller reference early

            Stage hostGameStage = new Stage();
            hostGameStage.setTitle("Host Chess Game");
            hostGameStage.setScene(new Scene(hostGameRoot));
            hostGameStage.initModality(Modality.WINDOW_MODAL);
            if (rootPane != null) {
                hostGameStage.initOwner(rootPane.getScene().getWindow());
            } else {
                System.err.println("Warning: rootPane is null. Host game window might not be modal.");
            }
            hostGameStage.showAndWait(); // Wait for the dialog to close

            // After the host game dialog closes, check if a connection was made
            if (hostGameController.isServerStartedSuccessfully()) {
                gameSocket = hostGameController.getClientSocket();
                if (gameSocket != null && !gameSocket.isClosed()) {
                    System.out.println("ChessController: Host server successfully started and client connected. Game ready!");
                    initializeGameWithNetwork(gameSocket, true);
                } else {
                    System.err.println("ChessController: Host dialog closed, but no valid client socket.");
                    showAlert("Connection Error", "Host server started, but failed to establish client connection.", Alert.AlertType.ERROR);
                }
            } else {
                System.out.println("ChessController: Host game setup cancelled or server failed to start.");
                showAlert("Game Setup", "Host game setup was cancelled or failed to start.", Alert.AlertType.INFORMATION);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load Host Game view: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleJoinGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/group13/chessgame/join-game-view.fxml"));
            Parent joinGameRoot = loader.load();
            JoinGameController joinGameController = loader.getController(); // Get controller reference early

            Stage joinGameStage = new Stage();
            joinGameStage.setTitle("Join Chess Game");
            joinGameStage.setScene(new Scene(joinGameRoot));
            joinGameStage.initModality(Modality.WINDOW_MODAL);
            if (rootPane != null) {
                joinGameStage.initOwner(rootPane.getScene().getWindow());
            } else {
                System.err.println("Warning: rootPane is null. Join game window might not be modal.");
            }
            joinGameStage.showAndWait(); // Wait for the dialog to close

            // After the join game dialog closes, check if a connection was made
            if (joinGameController.isConnectedSuccessfully()) {
                gameSocket = joinGameController.getClientSocket();
                if (gameSocket != null && !gameSocket.isClosed()) {
                    System.out.println("ChessController: Successfully connected to host. Game ready!");
                    initializeGameWithNetwork(gameSocket, false);
                } else {
                    System.err.println("ChessController: Join dialog closed, but no valid client socket.");
                    showAlert("Connection Error", "Successfully connected, but no valid client socket.", Alert.AlertType.ERROR);
                }
            } else {
                System.out.println("ChessController: Join game cancelled or failed to connect.");
                // The JoinGameController already updates its statusText with specific errors.
                // You could retrieve that status message if needed here, but the user already saw it.
                showAlert("Game Setup", "Join game was cancelled or failed to connect.", Alert.AlertType.INFORMATION);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load Join Game view: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Helper method to show alerts
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void initializeGameWithNetwork(Socket gameSocket, boolean isHost) {
        // Close any previous network connections if active
//        closeNetworkConnections();

//        chatHistoryTab.setDisable(false);
        this.gameSocket = gameSocket;
        this.isLanGameActive = true;
        this.myColor = isHost ? hostColor == PieceColor.WHITE ? PieceColor.WHITE : PieceColor.BLACK : hostColor == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;

        try {
            this.dataOutputStream = new DataOutputStream(gameSocket.getOutputStream());
            this.dataInputStream = new DataInputStream(gameSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Network Error", "Failed to setup network streams: " + e.getMessage(), Alert.AlertType.ERROR);
            // TODO: handle if failed to setup network streams
//            resetToLocalGame();
            return;
        }

        // Start listening for opponent's moves
        startNetworkListener();

        // Initialize game model and UI elements as usual
        this.gameModel = new Game();
        this.squarePanes = new StackPane[Board.SIZE][Board.SIZE];
        moveHistoryListView.setItems(moveHistoryObservableList);
        setupMoveHistoryCellFactory();
        initializeBoardGrid();
        loadSounds();
        setupPgnHeaderListeners();

        startNewGame();


        chatHistoryListView.setItems(chatHistoryObservableList);

        // Set board perspective based on player's color
        setBoardVisualPerspective(myColor);

        undoMenuItem.setDisable(false);
        redoMenuItem.setVisible(false);
        undoMoveButton.setVisible(false);
        redoMoveButton.setVisible(false);


        // If it's not my turn, disable input until a move is received
        if (gameModel.getCurrentPlayer().getColor() != myColor) {
            updateStatusLabel("Waiting for " + gameModel.getCurrentPlayer().getColor() + "'s move...");
        } else {
            updateStatusLabel("Your turn (" + myColor + ")");
        }

        System.out.println("Network game started! My color: " + myColor);
    }

    // Helper to find a legal move by its Standard Algebraic Notation (SAN)
    // This is a simplified placeholder. A robust network game would send coordinates.
    private Move findMoveBySAN(String san) {
        List<Move> legalMoves = gameModel.getAllLegalMovesForPlayer(gameModel.getCurrentPlayer().getColor());
        for (Move move : legalMoves) {
            // Need to compute SAN for each legal move and compare.
            // This can be computationally expensive if done repeatedly.
            // A better network protocol sends exact coordinates.
            String currentMoveSan = org.group13.chessgame.utils.NotationUtils.moveToAlgebraic(move, gameModel); // Re-compute SAN
            if (currentMoveSan.equals(san)) {
                return move;
            }
            // For promotions, SAN might look like "e8=Q"
            if (move.isPromotion() && san.equals(currentMoveSan + "=" + move.getPromotionPieceType().name().charAt(0))) {
                return move;
            }
        }
        return null;
    }


    // New: Starts a background task to listen for network moves
    private void startNetworkListener() {
        if (networkListenerTask != null && networkListenerTask.isRunning()) {
            networkListenerTask.cancel();
        }

        networkListenerTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    while (!isCancelled() && gameSocket != null && !gameSocket.isClosed()) {
                        // Read the move from the opponent as a string (e.g., SAN or FEN)
                        int messageType = dataInputStream.readInt();
                        if (messageType == NETWORK_MOVE_CODE) {
                            String opponentMoveSan = dataInputStream.readUTF();
                            System.out.println("Received move from opponent: " + opponentMoveSan);

                            // Apply the move on the JavaFX Application Thread
                            Platform.runLater(() -> {
                                try {
                                    Move opponentMove = findMoveBySAN(opponentMoveSan);
                                    if (opponentMove != null) {
                                        performMoveLogic(opponentMove); // Apply the move
                                        updateStatusLabel("Opponent played: " + opponentMoveSan + ". Your turn.");
                                        updateStatusBasedOnGameState();
                                    } else {
                                        System.err.println("Failed to parse or find opponent's move: " + opponentMoveSan);
                                        showAlert("Network Error", "Received invalid move from opponent.", Alert.AlertType.ERROR);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    showAlert("Network Error", "Error applying opponent's move: " + e.getMessage(), Alert.AlertType.ERROR);
                                }
                            });
                        } else if (messageType == NETWORK_SURRENDER_WHITE_CODE) {
                            Platform.runLater(() -> {
                                updateStatusBasedOnGameState();
                                surrenderButton.setDisable(true);
                                gameModel.surrender(PieceColor.WHITE);
                                System.out.println("Surrender initiated by " + PieceColor.WHITE);

                                // Cập nhật giao diện
                                updateStatusBasedOnGameState();
                                updateTurnLabel();
                                refreshBoardView();
                                boardGridPane.setMouseTransparent(true); // Khóa bàn cờ
                                undoMoveButton.setDisable(true);
                                surrenderButton.setDisable(true);

                                // Phát âm thanh
                                playSound(endGameSoundPlayer);
//                                closeNetworkConnections();
                            });

                        } else if (messageType == NETWORK_SURRENDER_BLACK_CODE) {
                            Platform.runLater(() -> {
                                updateStatusBasedOnGameState();
                                surrenderButton.setDisable(true);
                                gameModel.surrender(PieceColor.BLACK);
                                System.out.println("Surrender initiated by " + PieceColor.BLACK);

                                // Cập nhật giao diện
                                updateStatusBasedOnGameState();
                                updateTurnLabel();
                                refreshBoardView();
                                boardGridPane.setMouseTransparent(true); // Khóa bàn cờ
                                undoMoveButton.setDisable(true);
                                surrenderButton.setDisable(true);

                                // Phát âm thanh
                                playSound(endGameSoundPlayer);
//                                closeNetworkConnections();
                            });
                        } else if (messageType == NETWORK_CHAT_CODE) {
                            String messageContent = dataInputStream.readUTF();
                            chatHistoryListView.getItems().add(messageContent);
                            chatHistoryListView.refresh();
                        }
                    }
                } catch (EOFException e) {
                    Platform.runLater(() -> {
                        System.out.println("Opponent disconnected (EOF).");
                        showAlert("Disconnected", "Opponent disconnected from the game.", Alert.AlertType.INFORMATION);
                        // TODO: handle logic when opponent disconnected from the game
//                        resetToLocalGame();
                    });
                } catch (IOException e) {
                    if (!isCancelled()) { // Only report if not intentionally cancelled
                        Platform.runLater(() -> {
                            System.err.println("Network listener error: " + e.getMessage());
                            showAlert("Network Error", "Network communication error: " + e.getMessage(), Alert.AlertType.ERROR);
                            // TODO: handle logic when network communication is error.
//                            resetToLocalGame();
                        });
                    }
                }
                return null;
            }
        };

        networkListenerTask.setOnFailed(e -> {
            // Task failed (uncaught exception in call())
            System.err.println("Network listener task failed: " + e.getSource().getException().getMessage());
            showAlert("Network Error", "An unhandled error occurred in the network listener.", Alert.AlertType.ERROR);
            // TODO: handle logic when Network listener task failed
//            resetToLocalGame();
        });

        Thread listenerThread = new Thread(networkListenerTask);
        listenerThread.setDaemon(true); // Allow application to exit even if this thread is running
        listenerThread.start();
    }

    // New: Helper to set board visual orientation
    private void setBoardVisualPerspective(PieceColor perspective) {
        // Set internal flag for refreshBoardView and getModelSquare
        this.boardIsFlipped = (perspective == PieceColor.BLACK);
        refreshBoardView(); // Re-render the board with the new perspective
        System.out.println("Board visual perspective set to: " + perspective);

        // Update player labels to reflect perspective
        if (perspective == PieceColor.WHITE) {
            whitePlayerNameLabel.setText("You (White)");
            blackPlayerNameLabel.setText("Opponent (Black)");
            blackPlayerArea.setAlignment(Pos.CENTER_RIGHT);
            whitePlayerArea.setAlignment(Pos.CENTER_LEFT);
        } else { // BLACK perspective
            whitePlayerNameLabel.setText("Opponent (White)");
            blackPlayerNameLabel.setText("You (Black)");
            blackPlayerArea.setAlignment(Pos.CENTER_LEFT);
            whitePlayerArea.setAlignment(Pos.CENTER_RIGHT);
        }
    }

    @FXML
    private void handleSendChatMessage() {
        String message = chatInputField.getText();
        if (message != null && !message.trim().isEmpty()) {
            // Add the message to the chat history ListView
            chatHistoryListView.getItems().add("You: " + message.trim());
            chatInputField.clear(); // Clear the input field

            try {
                dataOutputStream.writeInt(NETWORK_CHAT_CODE);
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Sent chat message: " + message.trim());
        }
    }

    @FXML
    private void handleChatInputFieldKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleSendChatMessage();
            event.consume(); // Consume the event to prevent default behavior (like new line in some text areas)
        }
    }

    // New: Close network streams and socket
    private void closeNetworkConnections() {
        if (networkListenerTask != null) {
            networkListenerTask.cancel(); // Stop the listener task
            networkListenerTask = null;
        }
        try {
            if (dataInputStream != null) {
                dataInputStream.close();
                dataInputStream = null;
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
                dataOutputStream = null;
            }
            if (gameSocket != null && !gameSocket.isClosed()) {
                gameSocket.close();
                gameSocket = null;
                System.out.println("Network connections closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing network connections: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        this.gameModel = new Game();
        this.squarePanes = new StackPane[Board.SIZE][Board.SIZE];

        initializeBoardGrid();
        loadSounds();

        uciService = new UciService("engines/stockfish.exe");
        uciService.startEngine().thenAccept(started -> {
            if (!started) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Could not start chess engine. Player vs Computer mode will be unavailable.").show());
            }
        });

        moveHistoryListView.setItems(moveHistoryObservableList);
        setupMoveHistoryCellFactory();
        setupPgnHeaderListeners();
        startNewGame();
    }

    public void shutdown() {
        if (uciService != null) {
            uciService.stopEngine();
        }
    }

    private void initializeBoardGrid() {
        boardGridPane.getChildren().clear();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                StackPane squarePane = new StackPane();
                squarePane.setPrefSize(SQUARE_SIZE, SQUARE_SIZE);
                squarePane.setAlignment(Pos.CENTER);

                squarePane.setStyle("-fx-background-color: transparent;");

                Rectangle backgroundRect = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                if ((row + col) % 2 == 0) {
                    backgroundRect.getStyleClass().add("light-square-background");
                } else {
                    backgroundRect.getStyleClass().add("dark-square-background");
                }
                squarePane.getChildren().add(backgroundRect);

                Rectangle hoverOverlay = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                hoverOverlay.setMouseTransparent(true);
                hoverOverlay.getStyleClass().add("hovered-square-overlay");
                hoverOverlay.setVisible(false);
                squarePane.getChildren().add(hoverOverlay);

                Rectangle selectedOverlay = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                selectedOverlay.setMouseTransparent(true);
                selectedOverlay.getStyleClass().add("selected-square-overlay");
                selectedOverlay.setVisible(false);
                squarePane.getChildren().add(selectedOverlay);

                Rectangle dragOverlay = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                dragOverlay.setMouseTransparent(true);
                dragOverlay.getStyleClass().add("drag-over-square-overlay");
                dragOverlay.setVisible(false);
                squarePane.getChildren().add(dragOverlay);

                ImageView pieceImageView = new ImageView();
                pieceImageView.setFitWidth(SQUARE_SIZE * 0.8);
                pieceImageView.setFitHeight(SQUARE_SIZE * 0.8);
                pieceImageView.setPreserveRatio(true);
                squarePane.getChildren().add(pieceImageView);

                javafx.scene.shape.Circle moveIndicator = new javafx.scene.shape.Circle(SQUARE_SIZE / 5.0);
                moveIndicator.getStyleClass().add("move-indicator-dot");
                moveIndicator.setVisible(false);
                moveIndicator.setMouseTransparent(true);
                squarePane.getChildren().add(moveIndicator);

                squarePanes[row][col] = squarePane;
                boardGridPane.add(squarePane, col, row);

                addDragAndDropHandlers(squarePane, pieceImageView);
                final int r = row;
                final int c = col;
                squarePane.setOnMouseClicked(event -> {
                    if (!(isLanGameActive && myColor != gameModel.getCurrentPlayer().getColor()))
                        handleSquareClick(r, c);
                });
            }
        }
    }

    private Square getModelSquare(StackPane squarePane) {
        int targetViewRow = GridPane.getRowIndex(squarePane);
        int targetViewCol = GridPane.getColumnIndex(squarePane);
        int targetModelRow = boardIsFlipped ? (Board.SIZE - 1 - targetViewRow) : targetViewRow;
        int targetModelCol = boardIsFlipped ? (Board.SIZE - 1 - targetViewCol) : targetViewCol;
        return gameModel.getBoard().getSquare(targetModelRow, targetModelCol);
    }

    private void setupMoveHistoryCellFactory() {
        moveHistoryListView.setCellFactory(listView -> new MoveListCell(this));
    }

    private void setupPgnHeaderListeners() {
        whitePlayerField.textProperty().addListener((obs, oldText, newText) -> {
            if (gameModel != null && gameModel.getPgnHeaders() != null) {
                gameModel.getPgnHeaders().setWhite(newText);
            }
        });

        blackPlayerField.textProperty().addListener((obs, oldText, newText) -> {
            if (gameModel != null && gameModel.getPgnHeaders() != null) {
                gameModel.getPgnHeaders().setBlack(newText);
            }
        });

        eventField.textProperty().addListener((obs, oldText, newText) -> {
            if (gameModel != null && gameModel.getPgnHeaders() != null) {
                gameModel.getPgnHeaders().setEvent(newText);
            }
        });

        siteField.textProperty().addListener((obs, oldText, newText) -> {
            if (gameModel != null && gameModel.getPgnHeaders() != null) {
                gameModel.getPgnHeaders().setSite(newText);
            }
        });

        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (gameModel != null && gameModel.getPgnHeaders() != null && newDate != null) {
                String pgnDate = newDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                gameModel.getPgnHeaders().setDate(pgnDate);
            }
        });
    }

    private void refreshBoardView() {
        Board currentBoard = gameModel.getBoard();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                int viewRow = boardIsFlipped ? (Board.SIZE - 1 - row) : row;
                int viewCol = boardIsFlipped ? (Board.SIZE - 1 - col) : col;

                StackPane squarePane = squarePanes[viewRow][viewCol];
                ImageView pieceImageView = getPieceImageViewFromPane(squarePane);

                Piece piece = currentBoard.getPiece(row, col);
                if (piece != null) {
                    try {
                        String imagePath = piece.getImagePath();
                        Image pieceImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
                        pieceImageView.setImage(pieceImage);
                        pieceImageView.setVisible(true);
                    } catch (Exception e) {
                        System.err.println("Error loading image: " + piece.getImagePath() + " - " + e.getMessage());
                        pieceImageView.setImage(null);
                        pieceImageView.setVisible(false);
                    }
                } else {
                    pieceImageView.setImage(null);
                    pieceImageView.setVisible(false);
                }
                removeHighlightStyling(squarePane);
            }
        }
    }

    private void rebuildMoveHistoryView() {
        moveHistoryObservableList.clear();
        List<Move> playedMoves = gameModel.getPlayedMoveSequence();
        for (int i = 0; i < playedMoves.size(); i++) {
            Move whiteMove = playedMoves.get(i);
            if (whiteMove.getPieceMoved().getColor() == PieceColor.WHITE) {
                Move blackMove = null;
                if (i + 1 < playedMoves.size()) {
                    blackMove = playedMoves.get(i + 1);
                }
                moveHistoryObservableList.add(new MovePairDisplay((i / 2) + 1, whiteMove, blackMove));
                if (blackMove != null) {
                    i++;
                }
            } else {
                moveHistoryObservableList.add(new MovePairDisplay((i / 2) + 1, null, whiteMove));
            }
        }
        updateMoveHistoryViewHighlightAndScroll();
    }

    private void updateAllUIStates() {
        refreshBoardView();
        updateTurnLabel();
        updateStatusBasedOnGameState();
        updateUndoRedoButtonStates();
        updateActionButtonsState();
        updateCapturedPiecesView();
        rebuildMoveHistoryView();
        updatePgnHeaderFieldsFromResult();
        updateMoveHistoryViewHighlightAndScroll();
        autoFlipBoardButton.setVisible(currentMode == GameMode.ANALYSIS);

    }

    private void updatePgnHeaderFields(PgnHeaders headers) {
        if (headers == null) {
            whitePlayerField.setText("White Player");
            blackPlayerField.setText("Black Player");
            eventField.setText("Casual Game");
            siteField.setText("Local");
            datePicker.setValue(LocalDate.now());
            resultLabel.setText("*");
        } else {
            whitePlayerField.setText(headers.getWhite());
            blackPlayerField.setText(headers.getBlack());
            eventField.setText(headers.getEvent());
            siteField.setText(headers.getSite());
            try {
                datePicker.setValue(LocalDate.parse(headers.getDate(), DateTimeFormatter.ofPattern("yyyy.MM.dd")));
            } catch (Exception e) {
                datePicker.setValue(LocalDate.now());
                System.err.println("Could not parse PGN date: " + headers.getDate());
            }
            resultLabel.setText(headers.getResult());
        }
    }

    private PgnHeaders getCurrentHeadersFromFields() {
        PgnHeaders headers = new PgnHeaders();
        headers.setWhite(whitePlayerField.getText());
        headers.setBlack(blackPlayerField.getText());
        headers.setEvent(eventField.getText());
        headers.setSite(siteField.getText());
        if (datePicker.getValue() != null) {
            headers.setDate(datePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        } else {
            headers.setDate("????.??.??");
        }
        headers.setResult(resultLabel.getText());
        return headers;
    }

    private void updatePgnHeaderFieldsFromResult() {
        resultLabel.setText(getPgnResult(gameModel.getGameState()));
    }

    private void updateTurnLabel() {
        turnLabel.setText(gameModel.getCurrentPlayer().getColor() + " to move");
    }

    private void updateStatusLabel(String message) {
        statusLabel.setText(message);
    }

    private void updateStatusBasedOnGameState() {
        String status;
        Game.GameState currentState = gameModel.getGameState();
        status = switch (currentState) {
            case CHECK -> gameModel.getCurrentPlayer().getColor() + " is in Check!";
            case WHITE_WINS_CHECKMATE -> "Checkmate! WHITE wins.";
            case BLACK_WINS_CHECKMATE -> "Checkmate! BLACK wins.";
            case WHITE_SURRENDERS -> "BLACK wins by surrender.";
            case BLACK_SURRENDERS -> "WHITE wins by surrender.";
            case STALEMATE_DRAW -> "Stalemate! It's a draw.";
            case FIFTY_MOVE_DRAW -> "Draw by 50-move rule.";
            case THREEFOLD_REPETITION_DRAW -> "Draw by threefold repetition.";
            case INSUFFICIENT_MATERIAL_DRAW -> "Draw by insufficient material.";
            default -> "";
        };
        statusLabel.setText(status);
        resultLabel.setText(getPgnResult(gameModel.getGameState()));
        if (currentState != Game.GameState.ACTIVE && currentState != Game.GameState.CHECK) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Over");
                alert.setHeaderText(null);
                alert.setContentText(status);
                alert.show();
            });
        }
    }

    private void updateUndoRedoButtonStates() {
        boolean isPlayerTurn = currentMode == GameMode.ANALYSIS || gameModel.getCurrentPlayer().getColor() == playerColor;

        undoMoveButton.setDisable(!gameModel.canUndo() || !isPlayerTurn);
        undoMenuItem.setDisable(!gameModel.canUndo() || !isPlayerTurn);

        redoMoveButton.setDisable(!gameModel.canRedo() || !isPlayerTurn);
        redoMenuItem.setDisable(!gameModel.canRedo() || !isPlayerTurn);
    }

    private void updateActionButtonsState() {
        boolean isGameOver = isGameOver();
        boolean isPlayerTurn = currentMode == GameMode.ANALYSIS || gameModel.getCurrentPlayer().getColor() == playerColor;

        offerDrawMenuItem.setDisable(!isGameOver && (currentMode == GameMode.HOSTING || currentMode == GameMode.JOINING || currentMode == GameMode.ANALYSIS));
        offerDrawButton.setDisable(!isGameOver && (currentMode == GameMode.HOSTING || currentMode == GameMode.JOINING || currentMode == GameMode.ANALYSIS));

        surrenderMenuItem.setDisable(isGameOver || !isPlayerTurn);
        surrenderButton.setDisable(isGameOver || !isPlayerTurn);

        moveHistoryListView.setDisable(!isPlayerTurn);
    }

    private void updateCapturedPiecesView() {
        capturedByWhiteArea.getChildren().clear();
        capturedByBlackArea.getChildren().clear();

        Comparator<Piece> pieceComparator = Comparator.comparingInt((Piece p) -> getPieceValue(p.getType())).reversed().thenComparing(p -> p.getType().toString());

        List<Piece> whiteCaptures = new ArrayList<>(gameModel.getCapturedPieces(PieceColor.WHITE));
        populateCapturedPiecesPane(pieceComparator, whiteCaptures, capturedByWhiteArea);

        List<Piece> blackCaptures = new ArrayList<>(gameModel.getCapturedPieces(PieceColor.BLACK));
        populateCapturedPiecesPane(pieceComparator, blackCaptures, capturedByBlackArea);
    }

    private void updateMoveHistoryViewHighlightAndScroll() {
        moveHistoryListView.refresh();
        if (currentPlyPointer >= 0) {
            int displayIndex = currentPlyPointer / 2;
            if (displayIndex < moveHistoryObservableList.size()) {
                javafx.application.Platform.runLater(() -> moveHistoryListView.scrollTo(displayIndex));
            }
        }
    }

    @FXML
    private void handleNewGame() {
        if (gameModel.canUndo()) {
            Alert confirmSave = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save the current game before starting a new one?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            confirmSave.setTitle("Save Current Game?");
            Optional<ButtonType> result = confirmSave.showAndWait();

            if (result.isPresent()) {
                if (result.get() == ButtonType.YES) {
                    handleSaveGame();
                } else if (result.get() == ButtonType.CANCEL) {
                    return;
                }
            } else {
                return;
            }
        }

        Optional<GameSetupResult> result = showGameSetupDialog();
        result.ifPresent(setup -> startNewGame(setup.mode, setup.playerColor, setup.difficulty));
    }

    private Optional<GameSetupResult> showGameSetupDialog() {
        Dialog<GameSetupResult> dialog = new Dialog<>();
        dialog.setTitle("New Game Setup");
        dialog.setHeaderText("Choose your game settings");

        ButtonType startButtonType = new ButtonType("Start Game", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(startButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton pvcRadio = new RadioButton("Player vs Computer");
        pvcRadio.setToggleGroup(modeGroup);

        RadioButton aRadio = new RadioButton("2 Players Mode");
        aRadio.setToggleGroup(modeGroup);
        aRadio.setSelected(true);

        RadioButton hostGameRadio = new RadioButton("Host game");
        RadioButton joinGameRadio = new RadioButton("Join game");
        hostGameRadio.setToggleGroup(modeGroup);
        joinGameRadio.setToggleGroup(modeGroup);

        Label colorLabel = new Label("Play as:");
        ToggleGroup colorGroup = new ToggleGroup();
        RadioButton whiteRadio = new RadioButton("White");
        whiteRadio.setToggleGroup(colorGroup);
        whiteRadio.setSelected(true);
        RadioButton blackRadio = new RadioButton("Black");
        blackRadio.setToggleGroup(colorGroup);
        RadioButton randomRadio = new RadioButton("Random");
        randomRadio.setToggleGroup(colorGroup);
        HBox colorBox = new HBox(10, whiteRadio, blackRadio, randomRadio);

        Label difficultyLabel = new Label("Difficulty:");
        ComboBox<Difficulty> difficultyComboBox = new ComboBox<>();
        difficultyComboBox.getItems().setAll(Difficulty.values());
        difficultyComboBox.setValue(Difficulty.MEDIUM);

        colorLabel.setDisable(true);
        colorBox.setDisable(true);
        difficultyLabel.setDisable(true);
        difficultyComboBox.setDisable(true);

        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isPvc = (newVal == pvcRadio);
            boolean isHosting = (newVal == hostGameRadio);
            boolean isJoining = (newVal == joinGameRadio);
            boolean isPvp = (newVal == hostGameRadio || newVal == joinGameRadio);
            boolean isAnalysis = (newVal == aRadio);
            colorLabel.setDisable(isAnalysis || isJoining);
            colorBox.setDisable(isAnalysis || isJoining);
            difficultyLabel.setDisable(!isPvc);
            difficultyComboBox.setDisable(!isPvc);
        });

        grid.add(new Label("Game Mode:"), 0, 0);
        grid.add(aRadio, 1, 0);
        grid.add(pvcRadio, 2, 0);
        grid.add(hostGameRadio, 3, 0);
        grid.add(joinGameRadio, 4, 0);

        grid.add(colorLabel, 0, 1);
        grid.add(colorBox, 1, 1, 2, 1);

        grid.add(difficultyLabel, 0, 2);
        grid.add(difficultyComboBox, 1, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == startButtonType) {
                GameMode selectedMode;
                if (hostGameRadio.isSelected()) {
                    isPlayingPvp = true;
                    selectedMode = GameMode.HOSTING;
                } else if (joinGameRadio.isSelected()) {
                    isPlayingPvp = true;
                    selectedMode = GameMode.JOINING;
                } else if (pvcRadio.isSelected()) {
                    selectedMode = GameMode.PLAYER_VS_COMPUTER;
                    isPlayingPvp = false;
                } else {
                    selectedMode = GameMode.ANALYSIS;
                    isPlayingPvp = false;
                }
                PieceColor selectedColor = PieceColor.WHITE;
                if (randomRadio.isSelected()) {
                    Random random = new Random();
                    int randomNumber = random.nextInt(2);
                    System.out.println(randomNumber);
                    if (randomNumber == 0) {
                        selectedColor = PieceColor.WHITE;
                    } else if (randomNumber == 1) selectedColor = PieceColor.BLACK;
                } else {
                    selectedColor = whiteRadio.isSelected() ? PieceColor.WHITE : PieceColor.BLACK;
                }
                Difficulty selectedDifficulty = difficultyComboBox.getValue();
                return new GameSetupResult(selectedMode, selectedColor, selectedDifficulty);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void startNewGame() {
        startNewGame(GameMode.ANALYSIS, PieceColor.WHITE, Difficulty.MEDIUM);
    }

    private void startNewGame(GameMode mode, PieceColor playerSide, Difficulty difficulty) {
        this.currentMode = mode;
        this.playerColor = playerSide;
        this.currentDifficulty = difficulty;

        this.boardIsFlipped = playerSide == PieceColor.BLACK;
        prepareHeadersForNewGame(gameModel.getPgnHeaders());

        if (currentMode == GameMode.HOSTING) {
            this.hostColor = playerSide;
            handleHostGame();
            isPlayingPvp = true;
            chatHistoryTab.setDisable(false);
            offerDrawButton.setVisible(false);
            offerDrawMenuItem.setVisible(false);
        } else if (currentMode == GameMode.JOINING) {
            this.playerColor = hostColor == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
            handleJoinGame();
            isPlayingPvp = true;
            chatHistoryTab.setDisable(false);
            offerDrawButton.setVisible(false);
            offerDrawMenuItem.setVisible(false);
        } else {
            if (!isPlayingPvp) {
                isLanGameActive = false;
                offerDrawButton.setVisible(false);
                offerDrawMenuItem.setVisible(false);
            } else {
                isLanGameActive = true;
                offerDrawButton.setVisible(false);
                offerDrawMenuItem.setVisible(false);
            }
            chatHistoryTab.setDisable(true);
            gameModel.initializeGame();
            clearSelectionAndHighlights();
            currentPlyPointer = -1;
            updatePgnHeaderFields(gameModel.getPgnHeaders());
            updateAllUIStates();
            whiteCapturedPieces.clear();
            blackCapturedPieces.clear();

            if (currentMode == GameMode.PLAYER_VS_COMPUTER && gameModel.getCurrentPlayer().getColor() != playerColor) {
                requestEngineMove();
            }
        }
    }

    private void prepareHeadersForNewGame(PgnHeaders headers) {
        String engineDisplayName = (uciService != null) ? uciService.getEngineName() : "Computer";
        String currentPlayerName = "Player";

        try {
            int currentRound = Integer.parseInt(headers.getRound());
            headers.setRound(String.valueOf(currentRound + 1));
        } catch (NumberFormatException e) {
            headers.setRound("1");
        }
        datePicker.setValue(LocalDate.now());
        resultLabel.setText("*");

        if (currentMode == GameMode.PLAYER_VS_COMPUTER) {
            if (playerColor == PieceColor.WHITE) {
                whitePlayerField.setText(currentPlayerName);
                blackPlayerField.setText(engineDisplayName);
            } else {
                whitePlayerField.setText(engineDisplayName);
                blackPlayerField.setText(currentPlayerName);
            }
        } else {
            if (whitePlayerField.getText().equals(engineDisplayName)) whitePlayerField.setText("White Player");
            if (blackPlayerField.getText().equals(engineDisplayName)) blackPlayerField.setText("Black Player");
        }
    }

    @FXML
    private void handleUndoMove() {
        clearSelectionAndHighlights();
        Move undoneMove = gameModel.undo();
        if (undoneMove != null) {
            currentPlyPointer--;
            if (currentMode != GameMode.ANALYSIS && gameModel.canUndo()) {
                undoneMove = gameModel.undo();
                if (undoneMove != null) {
                    currentPlyPointer--;
                }
            }
        }
        updateAllUIStates();

        if (currentMode == GameMode.PLAYER_VS_COMPUTER && gameModel.getCurrentPlayer().getColor() != playerColor && !isGameOver()) {
            requestEngineMove();
        }
    }

    @FXML
    private void handleRedoMove() {
        clearSelectionAndHighlights();
        Move redoneMove = gameModel.redo();
        if (redoneMove != null) {
            currentPlyPointer++;
            redoneMove = gameModel.redo();
            if (redoneMove != null) {
                currentPlyPointer++;
            }
        }
        updateAllUIStates();

        if (currentMode == GameMode.PLAYER_VS_COMPUTER && gameModel.getCurrentPlayer().getColor() != playerColor && !isGameOver()) {
            requestEngineMove();
        }
    }

    @FXML
    private void handleAutoFlipBoard() {
        isAutoFlippingBoard = !isAutoFlippingBoard;
        autoFlipBoardButton.setText("Auto Flip Board: " + (isAutoFlippingBoard ? "ON" : "OFF"));

        if (isAutoFlippingBoard) {
            boardIsFlipped = !(gameModel.getCurrentPlayer().getColor() == PieceColor.WHITE);
            clearSelectionAndHighlights();
            refreshBoardView();
        }
    }

    @FXML
    private void handleFlipBoard() {
        boardIsFlipped = !boardIsFlipped;
        clearSelectionAndHighlights();
        refreshBoardView();
    }

    @FXML
    private void handleSaveGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Game as PGN");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PGN Files (*.pgn)", "*.pgn"));
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                PgnHeaders headers = getCurrentHeadersFromFields();
                headers.setResult(getPgnResult(gameModel.getGameState()));
                String pgnContent = PgnFormatter.formatGame(gameModel.getPgnHeaders(), gameModel.getPlayedMoveSequence(), gameModel.getGameState());
                writer.print(pgnContent);
                updateStatusLabel("Game saved as PGN: " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLoadGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Game from PGN");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PGN Files (*.pgn)", "*.pgn"));
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            try {
                String pgnString = new String(java.nio.file.Files.readAllBytes(file.toPath()));

                this.gameModel = PgnParser.parsePgn(pgnString);
                currentPlyPointer = gameModel.getPlayedMoveSequence().size() - 1;
                updatePgnHeaderFields(gameModel.getPgnHeaders());
                updateAllUIStates();
                updateStatusLabel("Game loaded from PGN: " + file.getName());

                if (currentMode == GameMode.PLAYER_VS_COMPUTER && gameModel.getCurrentPlayer().getColor() != playerColor && !isGameOver()) {
                    requestEngineMove();
                }
            } catch (IOException | PgnParseException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FXML
    private void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText("You are about to exit the game.");
        alert.setContentText("Are you sure you want to exit?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
        }
    }

    @FXML
    private void handleOfferDraw() {
    }

    @FXML
    private void handleSurrender() {
        if (isGameOver()) {
            System.out.println("Surrender blocked: Game is already over with state " + gameModel.getGameState());
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm surrender");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Do you really want to surrender?");
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            PieceColor surrenderPieceColor;
            if (isLanGameActive) {
                surrenderPieceColor = myColor;
                try {
                    dataOutputStream.writeInt(myColor == PieceColor.WHITE ? NETWORK_SURRENDER_WHITE_CODE : NETWORK_SURRENDER_BLACK_CODE);
                    dataOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else surrenderPieceColor = gameModel.getCurrentPlayer().getColor();
            String winner = (surrenderPieceColor == PieceColor.WHITE) ? "BLACK" : "WHITE";
            System.out.println("Surrender initiated by " + surrenderPieceColor);

            // Gọi phương thức surrender từ Game
            gameModel.surrender(surrenderPieceColor);

            updateAllUIStates();

            playSound(endGameSoundPlayer);
        }
    }

    private void handleSquareClick(int viewRow, int viewCol) {
        if (isGameOver()) {
            return;
        }
        if (currentMode != GameMode.ANALYSIS && gameModel.getCurrentPlayer().getColor() != playerColor) {
            return;
        }

        if (isLanGameActive && myColor != gameModel.getCurrentPlayer().getColor()) return;

        int modelRow = boardIsFlipped ? (Board.SIZE - 1 - viewRow) : viewRow;
        int modelCol = boardIsFlipped ? (Board.SIZE - 1 - viewCol) : viewCol;

        Square clickedBoardSquare = gameModel.getBoard().getSquare(modelRow, modelCol);

        if (selectedSquare == null) {
            if (clickedBoardSquare.hasPiece() && clickedBoardSquare.getPiece().getColor() == gameModel.getCurrentPlayer().getColor()) {
                selectPiece(clickedBoardSquare);
            }
        } else {
            Optional<Move> chosenMoveOpt = availableMovesForSelectedPiece.stream().filter(m -> m.getEndSquare() == clickedBoardSquare).findFirst();

            if (chosenMoveOpt.isPresent()) {
                Move chosenMove = chosenMoveOpt.get();

                if (chosenMove.getPieceMoved().getType() == PieceType.PAWN && (chosenMove.getEndSquare().getRow() == 0 || chosenMove.getEndSquare().getRow() == Board.SIZE - 1)) {

                    PieceType promotionType = askForPromotionType();
                    if (promotionType == null) {
                        clearSelectionAndHighlights();
                        return;
                    }
                    final PieceType finalPromotionType = promotionType;
                    chosenMove = availableMovesForSelectedPiece.stream().filter(m -> m.getEndSquare() == clickedBoardSquare && m.isPromotion() && m.getPromotionPieceType() == finalPromotionType).findFirst().orElseThrow(() -> new IllegalStateException("Selected promotion move not found in legal moves."));
                }

                clearSelectionAndHighlights();
                performMoveAnimation(chosenMove, true);

            } else if (clickedBoardSquare.hasPiece() && clickedBoardSquare.getPiece().getColor() == gameModel.getCurrentPlayer().getColor()) {
                clearSelectionAndHighlights();
                selectPiece(clickedBoardSquare);
            } else {
                clearSelectionAndHighlights();
            }
        }
    }

//    private void performMoveLogic(Move move) {
//        // Capture the SAN for network transmission *before* making the move on the model
//        String moveSanForNetwork = org.group13.chessgame.utils.NotationUtils.moveToAlgebraic(move, gameModel);
//
//        Move executedMove = gameModel.makeMove(move); // Apply the move to the model
//        if (executedMove != null) {
//            currentPlyPointer = gameModel.getPlayedMoveSequence().size() - 1;
//            rebuildMoveHistoryView();
//            refreshBoardView();
//            updateTurnLabel();
//            updateUndoRedoButtonStates();
//            Piece captured = executedMove.getPieceCaptured();
//            if (captured != null) {
//                if (captured.getColor() == PieceColor.BLACK) {
//                    whiteCapturedPieces.add(captured);
//                } else {
//                    blackCapturedPieces.add(captured);
//                }
//            }
//            updateCapturedPiecesView();
//            updateMoveHistoryViewHighlightAndScroll();
//            playMoveSounds(executedMove);
//
//            // If it's a LAN game and this is MY move, send it to the opponent
//            if (isLanGameActive && (executedMove.getPieceMoved().getColor() == myColor)) {
//                try {
//                    dataOutputStream.writeInt(NETWORK_MOVE_CODE);
//                    dataOutputStream.writeUTF(moveSanForNetwork); // Send SAN string
//                    dataOutputStream.flush();
//                    System.out.println("Sent move to opponent: " + moveSanForNetwork);
//                    updateStatusLabel("Waiting for opponent's move...");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    showAlert("Network Error", "Failed to send move to opponent: " + e.getMessage(), Alert.AlertType.ERROR);
//                    // TODO: handle logic when failed to send move

    /// /                    resetToLocalGame(); // Fallback to local game on network error
//                }
//            }
//
//            updateStatusBasedOnGameState();
//        } else {
//            updateStatusLabel("Error: Invalid move attempted!");
//            refreshBoardView();
//        }
//        clearSelectionAndHighlights();
//    }
    private void performMoveLogic(Move move) {
        if (currentMode == GameMode.PLAYER_VS_COMPUTER || currentMode == GameMode.ANALYSIS) {
            if (gameModel.canRedo()) {
                Move nextMoveInRedoStack = gameModel.getRedoStack().peek();

                if (move.isEquivalent(nextMoveInRedoStack)) {
                    handleRedoMove();
                    return;
                }
            }
        }

        // Capture the SAN for network transmission *before* making the move on the model
        String moveSanForNetwork = org.group13.chessgame.utils.NotationUtils.moveToAlgebraic(move, gameModel);

        Move executedMove = gameModel.makeMove(move);
        if (executedMove != null) {
            if (currentMode == GameMode.PLAYER_VS_COMPUTER && !isGameOver()) {
                requestEngineMove();
            }
            updateUIAfterMoving(executedMove);

            if (currentMode == GameMode.ANALYSIS && isAutoFlippingBoard) {
                boardIsFlipped = !(gameModel.getCurrentPlayer().getColor() == PieceColor.WHITE);
                clearSelectionAndHighlights();
                refreshBoardView();
            }

            System.out.println("isLanGameActive: " + isLanGameActive);
            if (isLanGameActive && (executedMove.getPieceMoved().getColor() == myColor)) {
                try {
                    dataOutputStream.writeInt(NETWORK_MOVE_CODE);
                    dataOutputStream.writeUTF(moveSanForNetwork); // Send SAN string
                    dataOutputStream.flush();
                    System.out.println("Sent move to opponent: " + moveSanForNetwork);
                    updateStatusLabel("Waiting for opponent's move...");
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Network Error", "Failed to send move to opponent: " + e.getMessage(), Alert.AlertType.ERROR);
                    // TODO: handle logic when failed to send move
//                    resetToLocalGame(); // Fallback to local game on network error
                }
            }

        } else {
            updateStatusLabel("Error: Invalid move attempted!");
            refreshBoardView();
        }
        clearSelectionAndHighlights();
    }

    private void requestEngineMove() {
        boardGridPane.setMouseTransparent(true);
        statusLabel.setText("Computer is thinking...");

        List<Move> currentHistory = new ArrayList<>(gameModel.getUndoStack());
        Collections.reverse(currentHistory);

        StringBuilder movesString = new StringBuilder();
        for (Move move : currentHistory) {
            movesString.append(" ").append(move.toString());
        }
        String positionCommand = "position startpos moves" + movesString;

        int moveTime = this.currentDifficulty.getMoveTimeMillis();

        uciService.findBestMove(positionCommand, moveTime).thenAccept(bestMoveUci -> Platform.runLater(() -> {
            Move moveMade = gameModel.makeMoveFromUCI(bestMoveUci);
            if (moveMade != null) {
                performMoveAnimation(moveMade, false);
            } else {
                statusLabel.setText("Error: Engine returned an invalid move!");
                boardGridPane.setMouseTransparent(false);
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                statusLabel.setText("Error communicating with engine.");
                boardGridPane.setMouseTransparent(false);
            });
            return null;
        });
    }

    private void performMoveAnimation(Move move, boolean isPlayerMove) {
        Square startSquareModel = move.getStartSquare();
        Square endSquareModel = move.getEndSquare();

        int startViewRow = boardIsFlipped ? (Board.SIZE - 1 - startSquareModel.getRow()) : startSquareModel.getRow();
        int startViewCol = boardIsFlipped ? (Board.SIZE - 1 - startSquareModel.getCol()) : startSquareModel.getCol();
        int endViewRow = boardIsFlipped ? (Board.SIZE - 1 - endSquareModel.getRow()) : endSquareModel.getRow();
        int endViewCol = boardIsFlipped ? (Board.SIZE - 1 - endSquareModel.getCol()) : endSquareModel.getCol();

        StackPane startPane = squarePanes[startViewRow][startViewCol];
        ImageView pieceToAnimate = getPieceImageViewFromPane(startPane);

        ImageView tempAnimatedPiece = new ImageView(pieceToAnimate.getImage());
        tempAnimatedPiece.setFitWidth(pieceToAnimate.getFitWidth());
        tempAnimatedPiece.setFitHeight(pieceToAnimate.getFitHeight());
        tempAnimatedPiece.setPreserveRatio(true);

        double startCellX = startViewCol * SQUARE_SIZE;

        double fromX = startCellX + (SQUARE_SIZE - tempAnimatedPiece.getFitWidth()) / 2;
        double fromY = startViewRow * SQUARE_SIZE;

        double endCellX = endViewCol * SQUARE_SIZE;

        double toX = endCellX + (SQUARE_SIZE - tempAnimatedPiece.getFitWidth()) / 2;
        double toY = endViewRow * SQUARE_SIZE;

        pieceToAnimate.setVisible(false);

        boardGridPane.getChildren().add(tempAnimatedPiece);
        tempAnimatedPiece.setLayoutX(0);
        tempAnimatedPiece.setLayoutY(0);
        tempAnimatedPiece.setTranslateX(fromX);
        tempAnimatedPiece.setTranslateY(fromY);

        TranslateTransition tt = new TranslateTransition(Duration.millis(250), tempAnimatedPiece);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.setFromX(fromX);
        tt.setFromY(fromY);
        tt.setToX(toX);
        tt.setToY(toY);

        boardGridPane.setMouseTransparent(true);

        tt.setOnFinished(event -> {
            boardGridPane.getChildren().remove(tempAnimatedPiece);
            if (isPlayerMove) {
                performMoveLogic(move);
            } else {
                updateUIAfterMoving(move);
            }
            boardGridPane.setMouseTransparent(false);
        });

        tt.play();
    }

    private void updateUIAfterMoving(Move move) {
        currentPlyPointer = gameModel.getUndoStack().size() - 1;
        Piece captured = move.getPieceCaptured();
        if (captured != null) {
            if (captured.getColor() == PieceColor.BLACK) {
                whiteCapturedPieces.add(captured);
            } else {
                blackCapturedPieces.add(captured);
            }
        }
        updateAllUIStates();
        playMoveSounds(move);
    }

    private void addDragAndDropHandlers(StackPane squarePane, ImageView pieceImageView) {
        squarePane.setOnMouseEntered(event -> {
            if (isGameOver()) return;

            Square currentHoveredModelSquare = getModelSquare(squarePane);

            boolean showHover = false;
            if (selectedSquare != null) {
                boolean isAvailableMoveTarget = availableMovesForSelectedPiece.stream().anyMatch(m -> m.getEndSquare() == currentHoveredModelSquare);
                if (isAvailableMoveTarget) {
                    showHover = true;
                }
            }
            if (currentHoveredModelSquare.hasPiece() && currentHoveredModelSquare.getPiece().getColor() == gameModel.getCurrentPlayer().getColor()) {
                showHover = true;
            }

            boolean isSelectedOnThisSquare = (squarePane.getChildren().size() > SELECTED_OVERLAY_INDEX && squarePane.getChildren().get(SELECTED_OVERLAY_INDEX).isVisible());
            boolean isDragOverOnThisSquare = (squarePane.getChildren().size() > DRAG_OVERLAY_INDEX && squarePane.getChildren().get(DRAG_OVERLAY_INDEX).isVisible());

            if (showHover && !isSelectedOnThisSquare && !isDragOverOnThisSquare) {
                setOverlayVisible(squarePane, HOVER_OVERLAY_INDEX, true);
            }
        });
        squarePane.setOnMouseExited(event -> setOverlayVisible(squarePane, HOVER_OVERLAY_INDEX, false));

        pieceImageView.setOnDragDetected(event -> {
            if (isGameOver()) return;

            if (isLanGameActive && myColor != gameModel.getCurrentPlayer().getColor()) return;

            StackPane sourcePane = (StackPane) pieceImageView.getParent();
            setOverlayVisible(sourcePane, HOVER_OVERLAY_INDEX, false);

            Square dragSourceSquareModel = getModelSquare(sourcePane);

            if (dragSourceSquareModel.hasPiece() && dragSourceSquareModel.getPiece().getColor() == gameModel.getCurrentPlayer().getColor()) {

                if (selectedSquare != null && selectedSquare != dragSourceSquareModel) {
                    clearSelectionAndHighlights();
                }

                selectPiece(dragSourceSquareModel);

                Dragboard db = pieceImageView.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(dragSourceSquareModel.getRow() + "," + dragSourceSquareModel.getCol());
                db.setContent(content);

                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);

                Image currentPieceImage = pieceImageView.getImage();
                if (currentPieceImage != null) {
                    Image dragViewImage = pieceImageView.snapshot(params, null);
                    db.setDragView(dragViewImage);
                    db.setDragViewOffsetX(dragViewImage.getWidth() / 2);
                    db.setDragViewOffsetY(dragViewImage.getHeight() / 2);
                }

                event.consume();
            }
        });

        squarePane.setOnDragOver(event -> {
            if (event.getGestureSource() != squarePane && event.getDragboard().hasString()) {
                boolean canDrop = availableMovesForSelectedPiece.stream().anyMatch(m -> m.getEndSquare() == getModelSquare(squarePane));

                if (canDrop) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
            }
            event.consume();
        });

        squarePane.setOnDragEntered(event -> {
            if (event.getGestureSource() != squarePane && event.getDragboard().hasString()) {
                if (availableMovesForSelectedPiece.stream().anyMatch(m -> m.getEndSquare() == getModelSquare(squarePane))) {
                    setOverlayVisible(squarePane, DRAG_OVERLAY_INDEX, true);
                    setOverlayVisible(squarePane, HOVER_OVERLAY_INDEX, false);
                }
            }
            event.consume();
        });

        squarePane.setOnDragExited(event -> {
            setOverlayVisible(squarePane, DRAG_OVERLAY_INDEX, false);
            event.consume();
        });

        squarePane.setOnDragDropped(event -> {
            if (currentMode != GameMode.ANALYSIS && gameModel.getCurrentPlayer().getColor() != playerColor) {
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String[] sourceCoords = db.getString().split(",");
                int sourceModelRow = Integer.parseInt(sourceCoords[0]);
                int sourceModelCol = Integer.parseInt(sourceCoords[1]);

                Square sourceDragModelSquare = gameModel.getBoard().getSquare(sourceModelRow, sourceModelCol);

                if (selectedSquare == sourceDragModelSquare) {
                    Optional<Move> chosenMoveOpt = availableMovesForSelectedPiece.stream().filter(m -> m.getEndSquare() == getModelSquare(squarePane)).findFirst();

                    if (chosenMoveOpt.isPresent()) {
                        Move moveToDo = chosenMoveOpt.get();
                        if (moveToDo.getPieceMoved().getType() == PieceType.PAWN && (moveToDo.getEndSquare().getRow() == 0 || moveToDo.getEndSquare().getRow() == (Board.SIZE - 1))) {

                            PieceType promotionChoice = askForPromotionType();
                            if (promotionChoice == null) {
                                clearSelectionAndHighlights();
                                event.setDropCompleted(false);
                                event.consume();
                                return;
                            }
                            final PieceType finalChoice = promotionChoice;
                            moveToDo = availableMovesForSelectedPiece.stream().filter(m -> m.getEndSquare() == getModelSquare(squarePane) && m.isPromotion() && m.getPromotionPieceType() == finalChoice).findFirst().orElseThrow(() -> new IllegalStateException("Selected promotion move (DnD) not found."));
                        }

                        performMoveLogic(moveToDo);
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
            if (!success) {
                clearSelectionAndHighlights();
            }
        });
    }

    private ImageView getPieceImageViewFromPane(StackPane pane) {
        for (Node node : pane.getChildren()) {
            if (node instanceof ImageView) {
                return (ImageView) node;
            }
        }
        System.err.println("Could not find piece ImageView in StackPane for refresh.");
        ImageView newImageView = new ImageView();
        pane.getChildren().add(newImageView);
        return newImageView;
    }

    private void selectPiece(Square squareToSelect) {
        clearSelectionAndHighlights();
        selectedSquare = squareToSelect;
        List<Move> filteredMoves = gameModel.getAllLegalMovesForPlayer(gameModel.getCurrentPlayer().getColor()).stream().filter(m -> m.getStartSquare() == selectedSquare).toList();
        availableMovesForSelectedPiece = new ArrayList<>(filteredMoves);
        int viewRow = boardIsFlipped ? (Board.SIZE - 1 - squareToSelect.getRow()) : squareToSelect.getRow();
        int viewCol = boardIsFlipped ? (Board.SIZE - 1 - squareToSelect.getCol()) : squareToSelect.getCol();
        highlightSelectedSquare(squarePanes[viewRow][viewCol]);
        highlightAvailableMoves();
    }

    private void loadSounds() {
        try {
            moveSoundPlayer = createMediaPlayer("/sound/Move.mp3");
            captureSoundPlayer = createMediaPlayer("/sound/Capture.mp3");
            checkSoundPlayer = createMediaPlayer("/sound/Check.mp3");
            endGameSoundPlayer = createMediaPlayer("/sound/Checkmate.mp3");
            castleSoundPlayer = createMediaPlayer("/sound/Move.mp3");
            promoteSoundPlayer = createMediaPlayer("/sound/Confirmation.mp3");
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }

    private MediaPlayer createMediaPlayer(String resourcePath) {
        URL resourceUrl = getClass().getResource(resourcePath);
        if (resourceUrl == null) {
            System.err.println("Cannot find sound resource: " + resourcePath);
            return null;
        }
        Media media = new Media(resourceUrl.toExternalForm());
        return new MediaPlayer(media);
    }

    private void playSound(MediaPlayer player) {
        if (player != null) {
            player.stop();
            player.seek(player.getStartTime());
            player.play();
        }
    }

    private void playMoveSounds(Move move) {
        if (move.isCastlingMove()) {
            playSound(castleSoundPlayer);
        } else if (move.isPromotion()) {
            playSound(promoteSoundPlayer);
        } else if (move.getPieceCaptured() != null) {
            playSound(captureSoundPlayer);
        } else {
            playSound(moveSoundPlayer);
        }

        Game.GameState currentState = gameModel.getGameState();
        if (currentState == Game.GameState.CHECK) {
            playSound(checkSoundPlayer);
        } else if (currentState == Game.GameState.WHITE_WINS_CHECKMATE || currentState == Game.GameState.BLACK_WINS_CHECKMATE || currentState == Game.GameState.WHITE_SURRENDERS || currentState == Game.GameState.BLACK_SURRENDERS || isDrawState(currentState)) {
            playSound(endGameSoundPlayer);
        }
    }

    private boolean isDrawState(Game.GameState state) {
        return state == Game.GameState.STALEMATE_DRAW || state == Game.GameState.FIFTY_MOVE_DRAW || state == Game.GameState.THREEFOLD_REPETITION_DRAW || state == Game.GameState.INSUFFICIENT_MATERIAL_DRAW || state == Game.GameState.DRAW_BY_AGREEMENT;
    }

    private PieceType askForPromotionType() {
        Dialog<PieceType> dialog = new Dialog<>();
        dialog.setTitle("Pawn Promotion");
        dialog.setHeaderText("Choose a piece to promote your pawn to:");

        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelButtonType);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20));

        PieceColor promotionColor = gameModel.getCurrentPlayer().getColor();
        PieceType[] promotionOptions = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};

        for (PieceType type : promotionOptions) {
            ImageView pieceView = PieceImageProvider.getImageViewFor(type, promotionColor, 50);
            Button choiceButton = new Button("", pieceView);
            choiceButton.setPrefSize(70, 70);
            choiceButton.setOnAction(event -> {
                dialog.setResult(type);
                dialog.close();
            });
            buttonBox.getChildren().add(choiceButton);
        }

        dialog.getDialogPane().setContent(buttonBox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == cancelButtonType) {
                return null;
            }
            return dialog.getResult();
        });

        Optional<PieceType> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void setOverlayVisible(StackPane pane, int overlayIndex, boolean visible) {
        if (pane != null && pane.getChildren().size() > overlayIndex) {
            Node overlayNode = pane.getChildren().get(overlayIndex);
            if (overlayNode != null) {
                overlayNode.setVisible(visible);
            }
        }
    }

    private void clearSelectionAndHighlights() {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                removeHighlightStyling(squarePanes[r][c]);
            }
        }
        selectedSquare = null;
        availableMovesForSelectedPiece.clear();
    }

    private void highlightSelectedSquare(StackPane pane) {
        setOverlayVisible(pane, HOVER_OVERLAY_INDEX, false);
        setOverlayVisible(pane, SELECTED_OVERLAY_INDEX, true);
    }

    private void highlightAvailableMoves() {
        for (Move move : availableMovesForSelectedPiece) {
            Square endModelSquare = move.getEndSquare();
            int endViewRow = boardIsFlipped ? (Board.SIZE - 1 - endModelSquare.getRow()) : endModelSquare.getRow();
            int endViewCol = boardIsFlipped ? (Board.SIZE - 1 - endModelSquare.getCol()) : endModelSquare.getCol();

            StackPane targetPane = squarePanes[endViewRow][endViewCol];
            if (targetPane.getChildren().size() > MOVE_INDICATOR_INDEX && targetPane.getChildren().get(MOVE_INDICATOR_INDEX) instanceof javafx.scene.shape.Circle indicator) {
                indicator.getStyleClass().clear();
                indicator.getStyleClass().add("move-indicator-dot");
                if (move.getPieceCaptured() != null || move.isEnPassantMove()) {
                    indicator.getStyleClass().add("move-indicator-dot-capture");
                } else {
                    indicator.getStyleClass().add("move-indicator-dot-normal");
                }
                indicator.setVisible(true);
            }
        }
    }

    private void removeHighlightStyling(StackPane pane) {
        setOverlayVisible(pane, HOVER_OVERLAY_INDEX, false);
        setOverlayVisible(pane, SELECTED_OVERLAY_INDEX, false);
        setOverlayVisible(pane, DRAG_OVERLAY_INDEX, false);
        setOverlayVisible(pane, MOVE_INDICATOR_INDEX, false);
    }

    private boolean isGameOver() {
        Game.GameState state = gameModel.getGameState();
        return state != Game.GameState.ACTIVE && state != Game.GameState.CHECK;
    }

    private void populateCapturedPiecesPane(Comparator<Piece> comparator, List<Piece> capturedList, FlowPane targetPane) {
        capturedList.sort(comparator);
        for (Piece captured : capturedList) {
            ImageView imgView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(captured.getImagePath()))));
            imgView.setFitHeight(SQUARE_SIZE * 0.35);
            imgView.setPreserveRatio(true);
            Tooltip.install(imgView, new Tooltip(captured.getColor() + " " + captured.getType()));
            targetPane.getChildren().add(imgView);
        }
    }

    private int getPieceValue(PieceType type) {
        return switch (type) {
            case QUEEN -> 9;
            case ROOK -> 5;
            case BISHOP -> 4;
            case KNIGHT -> 3;
            case PAWN -> 1;
            default -> 0;
        };
    }

    private String getPgnResult(Game.GameState state) {
        return switch (state) {
            case WHITE_WINS_CHECKMATE, BLACK_SURRENDERS -> "1-0";
            case BLACK_WINS_CHECKMATE, WHITE_SURRENDERS -> "0-1";
            case STALEMATE_DRAW, FIFTY_MOVE_DRAW, THREEFOLD_REPETITION_DRAW, INSUFFICIENT_MATERIAL_DRAW -> "1/2-1/2";
            default -> "*";
        };
    }

    private void jumpToMoveState(int targetPly) {
        if (targetPly == currentPlyPointer) return;
        clearSelectionAndHighlights();

        int steps = targetPly - currentPlyPointer;

        if (steps < 0) {
            for (int i = 0; i < Math.abs(steps); i++) {
                if (!gameModel.canUndo()) break;
                gameModel.undo();
            }
        } else {
            for (int i = 0; i < steps; i++) {
                if (!gameModel.canRedo()) break;
                gameModel.redo();
            }
        }
        currentPlyPointer = targetPly;

        updateAllUIStates();

        if (currentMode == GameMode.PLAYER_VS_COMPUTER && gameModel.getCurrentPlayer().getColor() != playerColor && !isGameOver()) {
            requestEngineMove();
        }
    }

    private enum GameMode {HOSTING, JOINING, PLAYER_VS_COMPUTER, ANALYSIS}

    private enum Difficulty {
        EASY(0), MEDIUM(100), HARD(5000);
        private final int moveTimeMillis;

        Difficulty(int moveTimeMillis) {
            this.moveTimeMillis = moveTimeMillis;
        }

        public int getMoveTimeMillis() {
            return moveTimeMillis;
        }
    }

    private record GameSetupResult(GameMode mode, PieceColor playerColor, Difficulty difficulty) {
    }

    private static class MoveListCell extends ListCell<MovePairDisplay> {
        private final HBox hbox = new HBox(5);
        private final Label moveNumberLabel = new Label();
        private final Label whiteMoveTextNode = new Label();
        private final Label blackMoveTextNode = new Label();

        private final ChessController controller;

        public MoveListCell(ChessController controller) {
            super();
            this.controller = controller;

            whiteMoveTextNode.getStyleClass().add("history-move-text");
            blackMoveTextNode.getStyleClass().add("history-move-text");
            moveNumberLabel.getStyleClass().add("history-move-number");

            whiteMoveTextNode.setOnMouseClicked(event -> {
                MovePairDisplay item = getItem();
                if (item != null && item.whiteMove() != null && event.getButton() == MouseButton.PRIMARY) {
                    int targetPlyIndex = controller.gameModel.getPlayedMoveSequence().indexOf(item.whiteMove());
                    if (targetPlyIndex != -1 && targetPlyIndex != controller.currentPlyPointer) {
                        controller.jumpToMoveState(targetPlyIndex);
                    } else if (targetPlyIndex == controller.currentPlyPointer) {
                        getListView().getSelectionModel().clearSelection();
                    }
                    event.consume();
                }
            });

            blackMoveTextNode.setOnMouseClicked(event -> {
                MovePairDisplay item = getItem();
                if (item != null && item.blackMove() != null && event.getButton() == MouseButton.PRIMARY) {
                    int targetPlyIndex = controller.gameModel.getPlayedMoveSequence().indexOf(item.blackMove());
                    if (targetPlyIndex != -1 && targetPlyIndex != controller.currentPlyPointer) {
                        controller.jumpToMoveState(targetPlyIndex);
                    } else if (targetPlyIndex == controller.currentPlyPointer) {
                        getListView().getSelectionModel().clearSelection();
                    }
                    event.consume();
                }
            });

            hbox.getChildren().addAll(moveNumberLabel, whiteMoveTextNode, blackMoveTextNode);
            hbox.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(MovePairDisplay item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                whiteMoveTextNode.getStyleClass().remove("current-move-text");
                blackMoveTextNode.getStyleClass().remove("current-move-text");
            } else {
                moveNumberLabel.setText(item.moveNumber() + ". ");

                String whiteSan = (item.whiteMove() != null && item.whiteMove().getStandardAlgebraicNotation() != null) ? item.whiteMove().getStandardAlgebraicNotation() : (item.whiteMove() != null ? "???" : "");
                whiteMoveTextNode.setText(whiteSan);
                whiteMoveTextNode.setVisible(item.whiteMove() != null);

                String blackSan = (item.blackMove() != null && item.blackMove().getStandardAlgebraicNotation() != null) ? item.blackMove().getStandardAlgebraicNotation() : (item.blackMove() != null ? "???" : "");
                blackMoveTextNode.setText(item.blackMove() != null ? blackSan : "");
                blackMoveTextNode.setVisible(item.blackMove() != null);

                setGraphic(hbox);

                whiteMoveTextNode.getStyleClass().remove("current-move-text");
                blackMoveTextNode.getStyleClass().remove("current-move-text");

                Move currentActualModelMove = (controller.currentPlyPointer >= 0 && controller.currentPlyPointer < controller.gameModel.getPlayedMoveSequence().size()) ? controller.gameModel.getPlayedMoveSequence().get(controller.currentPlyPointer) : null;

                if (item.whiteMove() != null && item.whiteMove() == currentActualModelMove) {
                    whiteMoveTextNode.getStyleClass().add("current-move-text");
                }
                if (item.blackMove() != null && item.blackMove() == currentActualModelMove) {
                    blackMoveTextNode.getStyleClass().add("current-move-text");
                }
            }
        }
    }
}