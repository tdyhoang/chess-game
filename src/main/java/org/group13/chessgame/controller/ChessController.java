package org.group13.chessgame.controller;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.group13.chessgame.model.*;
import org.group13.chessgame.utils.NotationUtils;
import org.group13.chessgame.utils.PieceImageProvider;

import java.net.URL;
import java.util.*;

public class ChessController {
    private static final int BOARD_DISPLAY_SIZE = 600;
    private static final int SQUARE_SIZE = 75;
    private final List<Piece> whiteCapturedPieces = new ArrayList<>();
    private final List<Piece> blackCapturedPieces = new ArrayList<>();
    private final ObservableList<String> moveHistoryObservableList = FXCollections.observableArrayList();
    @FXML
    private GridPane boardGridPane;
    @FXML
    private Label turnLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button undoMoveButton;
    @FXML
    private FlowPane capturedByWhitePane;
    @FXML
    private FlowPane capturedByBlackPane;
    @FXML
    private ListView<String> moveHistoryListView;
    private Game gameModel;
    private StackPane[][] squarePanes;
    private Square selectedSquare = null;
    private List<Move> availableMovesForSelectedPiece = new ArrayList<>();
    private MediaPlayer moveSoundPlayer, captureSoundPlayer, checkSoundPlayer, endGameSoundPlayer, castleSoundPlayer, promoteSoundPlayer;
    private boolean boardIsFlipped = false;

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
        clearSelectionAndHighlights();
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

    private void handleSquareClick(int viewRow, int viewCol) {
        if (isGameOver()) {
            return;
        }

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
                performMoveAnimation(chosenMove);

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

    private void performMoveAnimation(Move move) {
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
            performMoveLogic(move);
            boardGridPane.setMouseTransparent(false);
        });

        tt.play();
    }

    private void performMoveLogic(Move move) {
        boolean moveMade = gameModel.makeMove(move);
        if (moveMade) {
            addMoveToHistoryView(move);
            refreshBoardView();
            updateTurnLabel();
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
            playMoveSounds(move);
            updateStatusBasedOnGameState();
        } else {
            updateStatusLabel("Error: Invalid move attempted!");
            refreshBoardView();
        }
        clearSelectionAndHighlights();
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
        } else if (currentState == Game.GameState.WHITE_WINS_CHECKMATE || currentState == Game.GameState.BLACK_WINS_CHECKMATE || isDrawState(currentState)) {
            playSound(endGameSoundPlayer);
        }
    }

    private boolean isDrawState(Game.GameState state) {
        return state == Game.GameState.STALEMATE_DRAW || state == Game.GameState.FIFTY_MOVE_DRAW || state == Game.GameState.THREEFOLD_REPETITION_DRAW || state == Game.GameState.INSUFFICIENT_MATERIAL_DRAW;
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
            Square endModelSquare = move.getEndSquare();
            int endViewRow = boardIsFlipped ? (Board.SIZE - 1 - endModelSquare.getRow()) : endModelSquare.getRow();
            int endViewCol = boardIsFlipped ? (Board.SIZE - 1 - endModelSquare.getCol()) : endModelSquare.getCol();

            StackPane targetPane = squarePanes[endViewRow][endViewCol];
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
        String status;
        Game.GameState currentState = gameModel.getGameState();
        status = switch (currentState) {
            case CHECK -> gameModel.getCurrentPlayer().getColor() + " is in Check!";
            case WHITE_WINS_CHECKMATE -> "Checkmate! WHITE wins.";
            case BLACK_WINS_CHECKMATE -> "Checkmate! BLACK wins.";
            case STALEMATE_DRAW -> "Stalemate! It's a draw.";
            case FIFTY_MOVE_DRAW -> "Draw by 50-move rule.";
            case THREEFOLD_REPETITION_DRAW -> "Draw by threefold repetition.";
            case INSUFFICIENT_MATERIAL_DRAW -> "Draw by insufficient material.";
            default -> "";
        };
        statusLabel.setText(status);
        if (currentState != Game.GameState.ACTIVE && currentState != Game.GameState.CHECK) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Over");
                alert.setHeaderText(null);
                alert.setContentText(status);
                alert.showAndWait();
            });
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
        capturedByWhitePane.getChildren().clear();
        capturedByBlackPane.getChildren().clear();

        Comparator<Piece> pieceComparator = Comparator.comparingInt((Piece p) -> getPieceValue(p.getType())).reversed().thenComparing(p -> p.getType().toString());

        List<Piece> whiteCaptures = new ArrayList<>(gameModel.getCapturedPieces(PieceColor.WHITE));
        populateCapturedPiecesPane(pieceComparator, whiteCaptures, capturedByWhitePane);

        List<Piece> blackCaptures = new ArrayList<>(gameModel.getCapturedPieces(PieceColor.BLACK));
        populateCapturedPiecesPane(pieceComparator, blackCaptures, capturedByBlackPane);
    }

    private void populateCapturedPiecesPane(Comparator<Piece> comparator, List<Piece> capturedList, FlowPane targetPane) {
        capturedList.sort(comparator);
        for (Piece captured : capturedList) {
            ImageView imgView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(captured.getImagePath()))));
            imgView.getStyleClass().add("captured-piece-image");
            imgView.setFitHeight(SQUARE_SIZE * 0.35);
            imgView.setPreserveRatio(true);
            targetPane.getChildren().add(imgView);
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
                if (lastEntry.matches("^\\d+\\.\\s\\S+$")) {
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

    @FXML
    private void handleFlipBoard() {
        boardIsFlipped = !boardIsFlipped;
        refreshBoardView();
        if (selectedSquare != null) {
            clearSelectionAndHighlights();
        }
    }
}
