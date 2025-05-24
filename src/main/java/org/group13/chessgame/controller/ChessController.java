package org.group13.chessgame.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import org.group13.chessgame.model.*;
import org.group13.chessgame.utils.NotationUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChessController {
    private static final int BOARD_DISPLAY_SIZE = 600;
    private static final int SQUARE_SIZE = 75;
    private final List<Piece> whiteCapturedPieces = new ArrayList<>();
    private final List<Piece> blackCapturedPieces = new ArrayList<>();
    private final ObservableList<String> moveHistoryObservableList = FXCollections.observableArrayList();
    @FXML
    private GridPane boardGridPane;
    @FXML
    private ImageView boardBackgroundImage;
    @FXML
    private Label turnLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button undoMoveButton;
    @FXML
    private VBox capturedByWhiteArea;
    @FXML
    private VBox capturedByBlackArea;
    @FXML
    private ListView<String> moveHistoryListView;
    private Game gameModel;
    private StackPane[][] squarePanes;
    private Square selectedSquare = null;
    private List<Move> availableMovesForSelectedPiece = new ArrayList<>();
    private MediaPlayer moveSoundPlayer, captureSoundPlayer, checkSoundPlayer, endGameSoundPlayer, castleSoundPlayer, promoteSoundPlayer;

    @FXML
    public void initialize() {
        this.gameModel = new Game();
        this.squarePanes = new StackPane[Board.SIZE][Board.SIZE];
        moveHistoryListView.setItems(moveHistoryObservableList);
        initializeBoardGrid();
        loadSounds();
        startNewGame();
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
                squarePane.getStyleClass().add("chess-square-pane");

                Region selectionHighlightRegion = new Region();
                selectionHighlightRegion.setPrefSize(SQUARE_SIZE, SQUARE_SIZE);
                selectionHighlightRegion.getStyleClass().add("selected-square-highlight-border");
                selectionHighlightRegion.setVisible(false);
                selectionHighlightRegion.setMouseTransparent(true);
                squarePane.getChildren().add(selectionHighlightRegion);

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

                final int r = row;
                final int c = col;
                squarePane.setOnMouseClicked(event -> handleSquareClick(r, c));
            }
        }
    }

    private void startNewGame() {
        gameModel.initializeGame();
        selectedSquare = null;
        availableMovesForSelectedPiece.clear();
        moveHistoryObservableList.clear();
        refreshBoardView();
        updateTurnLabel();
        updateStatusLabel("");
        undoMoveButton.setDisable(gameModel.getMoveHistory().isEmpty());
        whiteCapturedPieces.clear();
        blackCapturedPieces.clear();
        updateCapturedPiecesView();
    }

    private void refreshBoardView() {
        Board currentBoard = gameModel.getBoard();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                StackPane squarePane = squarePanes[row][col];
                ImageView pieceImageView = getPieceImageViewFromPane(squarePane);

                Piece piece = currentBoard.getPiece(row, col);
                if (piece != null) {
                    try {
                        String imagePath = piece.getImagePath();
                        // getClass().getResourceAsStream()
                        Image pieceImage = new Image(getClass().getResourceAsStream(imagePath));
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

    private void handleSquareClick(int row, int col) {
        if (isGameOver()) {
            return;
        }

        Square clickedBoardSquare = gameModel.getBoard().getSquare(row, col);

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

                performMove(chosenMove);

            } else if (clickedBoardSquare.hasPiece() && clickedBoardSquare.getPiece().getColor() == gameModel.getCurrentPlayer().getColor()) {
                clearSelectionAndHighlights();
                selectPiece(clickedBoardSquare);
            } else {
                clearSelectionAndHighlights();
            }
        }
    }

    private void selectPiece(Square squareToSelect) {
        selectedSquare = squareToSelect;
        List<Move> filteredMoves = gameModel.getAllLegalMovesForPlayer(gameModel.getCurrentPlayer().getColor()).stream().filter(m -> m.getStartSquare() == selectedSquare).toList();
        availableMovesForSelectedPiece = new ArrayList<>(filteredMoves);
        highlightSelectedSquare(squarePanes[squareToSelect.getRow()][squareToSelect.getCol()]);
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

    private void performMove(Move move) {
        boolean moveMade = gameModel.makeMove(move);
        if (moveMade) {
            if (move.isCastlingMove()) {
                playSound(castleSoundPlayer);
            } else if (move.isPromotion()) {
                playSound(promoteSoundPlayer);
            } else if (move.getPieceCaptured() != null) {
                playSound(captureSoundPlayer);
            } else {
                playSound(moveSoundPlayer);
            }
            addMoveToHistoryView(move);
            refreshBoardView();
            updateTurnLabel();
            updateStatusBasedOnGameState();
            undoMoveButton.setDisable(gameModel.getMoveHistory().isEmpty() || isGameOver());
            Piece captured = move.getPieceCaptured();
            if (captured != null) {
                if (captured.getColor() == PieceColor.BLACK) {
                    whiteCapturedPieces.add(captured);
                } else {
                    blackCapturedPieces.add(captured);
                }
            }
            updateCapturedPiecesView();
        } else {
            updateStatusLabel("Error: Invalid move attempted!");
        }
        clearSelectionAndHighlights();
    }

    private PieceType askForPromotionType() {
        // TODO: Implement a proper JavaFX dialog for promotion choice
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Pawn Promotion");
        alert.setHeaderText("Choose a piece to promote to:");
        alert.setContentText("Choose your option.");

        ButtonType buttonTypeQueen = new ButtonType("Queen");
        ButtonType buttonTypeRook = new ButtonType("Rook");
        ButtonType buttonTypeBishop = new ButtonType("Bishop");
        ButtonType buttonTypeKnight = new ButtonType("Knight");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());

        alert.getButtonTypes().setAll(buttonTypeQueen, buttonTypeRook, buttonTypeBishop, buttonTypeKnight, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == buttonTypeQueen) return PieceType.QUEEN;
            if (result.get() == buttonTypeRook) return PieceType.ROOK;
            if (result.get() == buttonTypeBishop) return PieceType.BISHOP;
            if (result.get() == buttonTypeKnight) return PieceType.KNIGHT;
        }
        return null;
    }

    private void clearSelectionAndHighlights() {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                StackPane pane = squarePanes[r][c];
                if (pane.getChildren().size() > 1 && pane.getChildren().get(1) instanceof Region) {
                    pane.getChildren().get(1).setVisible(false);
                }
                if (pane.getChildren().size() > 3 && pane.getChildren().get(3) instanceof javafx.scene.shape.Circle) {
                    pane.getChildren().get(3).setVisible(false);
                }
            }
        }
        selectedSquare = null;
        availableMovesForSelectedPiece.clear();
    }

    private void highlightSelectedSquare(StackPane pane) {
        if (pane.getChildren().size() > 1 && pane.getChildren().get(1) instanceof Region borderRegion) {
            borderRegion.setVisible(true);
        }
    }

    private void highlightAvailableMoves() {
        for (Move move : availableMovesForSelectedPiece) {
            StackPane targetPane = squarePanes[move.getEndSquare().getRow()][move.getEndSquare().getCol()];
            if (targetPane.getChildren().size() > 3 && targetPane.getChildren().get(3) instanceof javafx.scene.shape.Circle indicator) {
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
        pane.getStyleClass().remove("selected-square-highlight-border");
        if (pane.getChildren().size() > 2 && pane.getChildren().get(2) instanceof javafx.scene.shape.Circle indicator) {
            indicator.setVisible(false);
            indicator.getStyleClass().remove("move-indicator-dot-capture");
            indicator.getStyleClass().remove("move-indicator-dot-normal");
        }
    }

    private boolean isGameOver() {
        Game.GameState state = gameModel.getGameState();
        return state != Game.GameState.ACTIVE && state != Game.GameState.CHECK;
    }

    private void updateTurnLabel() {
        turnLabel.setText(gameModel.getCurrentPlayer().getColor() + " to move");
    }

    private void updateStatusLabel(String message) {
        statusLabel.setText(message);
    }

    private void updateStatusBasedOnGameState() {
        String status = "";
        Game.GameState currentState = gameModel.getGameState();
        switch (currentState) {
            case CHECK:
                status = gameModel.getCurrentPlayer().getColor() + " is in Check!";
                break;
            case WHITE_WINS_CHECKMATE:
                status = "Checkmate! WHITE wins.";
                break;
            case BLACK_WINS_CHECKMATE:
                status = "Checkmate! BLACK wins.";
                break;
            case STALEMATE_DRAW:
                status = "Stalemate! It's a draw.";
                break;
            case FIFTY_MOVE_DRAW:
                status = "Draw by 50-move rule.";
                break;
            case THREEFOLD_REPETITION_DRAW:
                status = "Draw by threefold repetition.";
                break;
            case INSUFFICIENT_MATERIAL_DRAW:
                status = "Draw by insufficient material.";
                break;
            case ACTIVE:
            default:
                status = "";
                break;
        }
        statusLabel.setText(status);
        if (currentState == Game.GameState.CHECK) {
            playSound(checkSoundPlayer);
        } else if (currentState == Game.GameState.WHITE_WINS_CHECKMATE || currentState == Game.GameState.BLACK_WINS_CHECKMATE || currentState == Game.GameState.STALEMATE_DRAW || currentState == Game.GameState.FIFTY_MOVE_DRAW || currentState == Game.GameState.THREEFOLD_REPETITION_DRAW || currentState == Game.GameState.INSUFFICIENT_MATERIAL_DRAW) {
            playSound(endGameSoundPlayer);
        }
        if (currentState != Game.GameState.ACTIVE && currentState != Game.GameState.CHECK) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText(status);
            alert.showAndWait();
        }
    }

    @FXML
    private void handleNewGame() {
        startNewGame();
    }

    @FXML
    private void handleUndoMove() {
        if (gameModel.undoLastMove()) {
            removeLastMoveFromHistoryView();
            refreshBoardView();
            updateTurnLabel();
            updateStatusBasedOnGameState();
            undoMoveButton.setDisable(gameModel.getMoveHistory().isEmpty());
            updateCapturedPiecesView();
        }
    }

    private void updateCapturedPiecesView() {
        capturedByWhiteArea.getChildren().clear();
        capturedByBlackArea.getChildren().clear();

        List<Piece> capturedByWhite = gameModel.getCapturedPieces(PieceColor.WHITE);
        List<Piece> capturedByBlack = gameModel.getCapturedPieces(PieceColor.BLACK);

        List<Piece> sortedWhiteCaptures = new ArrayList<>(gameModel.getCapturedPieces(PieceColor.WHITE));
        // TODO: Implement comparator for sorting pieces by value (Q > R > B > N > P)
        for (Piece captured : sortedWhiteCaptures) {
            ImageView imgView = new ImageView(new Image(getClass().getResourceAsStream(captured.getImagePath())));
            imgView.getStyleClass().add("captured-piece-image");
            imgView.setFitHeight(SQUARE_SIZE * 0.4);
            imgView.setPreserveRatio(true);
            capturedByWhiteArea.getChildren().add(imgView);
        }

        List<Piece> sortedBlackCaptures = new ArrayList<>(gameModel.getCapturedPieces(PieceColor.BLACK));
        for (Piece captured : sortedBlackCaptures) {
            ImageView imgView = new ImageView(new Image(getClass().getResourceAsStream(captured.getImagePath())));
            imgView.getStyleClass().add("captured-piece-image");
            imgView.setFitHeight(SQUARE_SIZE * 0.4);
            imgView.setPreserveRatio(true);
            capturedByBlackArea.getChildren().add(imgView);
        }
    }

    private void addMoveToHistoryView(Move move) {
        String algebraicNotation = NotationUtils.moveToAlgebraic(move, gameModel);
        int moveNumber = (gameModel.getMoveHistory().size() + 1) / 2;

        if (move.getPieceMoved().getColor() == PieceColor.WHITE) {
            moveHistoryObservableList.add(moveNumber + ". " + algebraicNotation);
        } else {
            if (!moveHistoryObservableList.isEmpty()) {
                int lastIndex = moveHistoryObservableList.size() - 1;
                String lastEntry = moveHistoryObservableList.get(lastIndex);
                if (lastEntry.matches("^\\d+\\.\\s[^\\s]+$")) {
                    moveHistoryObservableList.set(lastIndex, lastEntry + "  " + algebraicNotation);
                } else {
                    moveHistoryObservableList.add(moveNumber + ". ... " + algebraicNotation);
                }
            } else {
                moveHistoryObservableList.add(moveNumber + ". ... " + algebraicNotation);
            }
        }
        moveHistoryListView.scrollTo(moveHistoryObservableList.size() - 1);
    }

    private void removeLastMoveFromHistoryView() {
        if (!moveHistoryObservableList.isEmpty()) {
            int lastIndex = moveHistoryObservableList.size() - 1;
            String lastEntry = moveHistoryObservableList.get(lastIndex);

            if (gameModel.getCurrentPlayer().getColor() == PieceColor.BLACK) {
                int secondMoveStartIndex = lastEntry.indexOf("  ");
                if (secondMoveStartIndex != -1 && lastEntry.substring(0, secondMoveStartIndex).contains(".")) {
                    moveHistoryObservableList.set(lastIndex, lastEntry.substring(0, secondMoveStartIndex));
                } else {
                    moveHistoryObservableList.remove(lastIndex);
                }
            } else {
                moveHistoryObservableList.remove(lastIndex);
            }
        }
        if (!moveHistoryObservableList.isEmpty()) {
            moveHistoryListView.scrollTo(moveHistoryObservableList.size() - 1);
        }
    }
}
