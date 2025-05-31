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
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.group13.chessgame.model.*;
import org.group13.chessgame.pgn.PgnHeaders;
import org.group13.chessgame.utils.PgnFormatter;
import org.group13.chessgame.utils.PieceImageProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
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
    private final List<Piece> whiteCapturedPieces = new ArrayList<>();
    private final List<Piece> blackCapturedPieces = new ArrayList<>();
    private final ObservableList<String> moveHistoryObservableList = FXCollections.observableArrayList();
    @FXML
    private BorderPane rootPane;
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

                final int r = row;
                final int c = col;
                squarePane.setOnMouseClicked(event -> handleSquareClick(r, c));
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
        String algebraicNotation = gameModel.getMoveHistory().getLast().getStandardAlgebraicNotation();
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
                PgnHeaders headers = new PgnHeaders();
                headers.setEvent("Casual Game");
                headers.setSite("Local Machine");
                headers.setDate(java.time.LocalDate.now().toString().replace("-", "."));
                headers.setRound("1");
                headers.setWhite(gameModel.getWhitePlayerInstance().getColor().toString());
                headers.setBlack(gameModel.getBlackPlayerInstance().getColor().toString());
                headers.setResult(getPgnResult(gameModel.getGameState()));

                String pgnContent = PgnFormatter.formatGame(headers, gameModel.getMoveHistory(), gameModel.getGameState());
                writer.print(pgnContent);
                updateStatusLabel("Game saved as PGN: " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getPgnResult(Game.GameState state) {
        return switch (state) {
            case WHITE_WINS_CHECKMATE -> "1-0";
            case BLACK_WINS_CHECKMATE -> "0-1";
            case STALEMATE_DRAW, FIFTY_MOVE_DRAW, THREEFOLD_REPETITION_DRAW, INSUFFICIENT_MATERIAL_DRAW -> "1/2-1/2";
            default -> "*";
        };
    }

    @FXML
    private void handleLoadGame() {
    }
}
