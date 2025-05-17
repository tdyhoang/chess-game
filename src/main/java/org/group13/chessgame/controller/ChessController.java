package org.group13.chessgame.controller;

import org.group13.chessgame.model.*;
// JavaFX imports
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChessController {
    @FXML private GridPane boardGridPane;
    @FXML private ImageView boardBackgroundImage;
    @FXML private Label turnLabel;
    @FXML private Label statusLabel;
    @FXML private Button undoMoveButton;
    @FXML private VBox capturedByWhiteBox;
    @FXML private VBox capturedByBlackBox;

    private Game gameModel;
    private StackPane[][] squarePanes;
    private static final int BOARD_DISPLAY_SIZE = 600;
    private static final int SQUARE_SIZE = 75;

    private Square selectedSquare = null;
    private List<Move> availableMovesForSelectedPiece = new ArrayList<>();

    @FXML
    public void initialize() {
        this.gameModel = new Game();
        this.squarePanes = new StackPane[Board.SIZE][Board.SIZE];

        try {
            Image bgImage = new Image(getClass().getResourceAsStream("/images/board/brown.png"));
            boardBackgroundImage.setImage(bgImage);
            boardGridPane.setPrefSize(BOARD_DISPLAY_SIZE, BOARD_DISPLAY_SIZE);
            boardGridPane.setMaxSize(BOARD_DISPLAY_SIZE, BOARD_DISPLAY_SIZE);
            boardBackgroundImage.setFitWidth(BOARD_DISPLAY_SIZE);
            boardBackgroundImage.setFitHeight(BOARD_DISPLAY_SIZE);

        } catch (Exception e) {
            System.err.println("Error loading board background image: " + e.getMessage());
        }

        initializeBoardGrid();
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

                Rectangle highlightBorder = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                highlightBorder.setFill(Color.TRANSPARENT);
                highlightBorder.setStroke(Color.TRANSPARENT);
                highlightBorder.setStrokeWidth(3);
                highlightBorder.setStrokeType(StrokeType.INSIDE);
                highlightBorder.setMouseTransparent(true);
                highlightBorder.setVisible(false);
                squarePane.getChildren().add(highlightBorder);

                ImageView pieceImageView = new ImageView();
                pieceImageView.setFitWidth(SQUARE_SIZE * 0.85);
                pieceImageView.setFitHeight(SQUARE_SIZE * 0.85);
                pieceImageView.setPreserveRatio(true);
                pieceImageView.setMouseTransparent(true);
                squarePane.getChildren().add(pieceImageView);

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
        refreshBoardView();
        updateTurnLabel();
        updateStatusLabel("");
        undoMoveButton.setDisable(gameModel.getMoveHistory().isEmpty());
        capturedByWhiteBox.getChildren().clear();
        capturedByBlackBox.getChildren().clear();
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
                removeHighlight(squarePane);
            }
        }
    }

    private ImageView getPieceImageViewFromPane(StackPane pane) {
        if (pane.getChildren().size() > 1 && pane.getChildren().get(1) instanceof ImageView) {
            return (ImageView) pane.getChildren().get(1);
        }
        if (pane.getChildren().size() > 2 && pane.getChildren().get(2) instanceof ImageView) {
            return (ImageView) pane.getChildren().get(2);
        }
        System.err.println("Could not find piece ImageView in StackPane for refresh.");
        ImageView newImageView = new ImageView();
        pane.getChildren().add(newImageView);
        return newImageView;
    }

    private void handleSquareClick(int row, int col) {
        if (gameModel.getGameState() != Game.GameState.ACTIVE && gameModel.getGameState() != Game.GameState.CHECK) {
            return;
        }

        Square clickedBoardSquare = gameModel.getBoard().getSquare(row, col);

        if (selectedSquare == null) {
            if (clickedBoardSquare.hasPiece() && clickedBoardSquare.getPiece().getColor() == gameModel.getCurrentPlayer().getColor()) {
                selectedSquare = clickedBoardSquare;
                availableMovesForSelectedPiece = gameModel.getAllLegalMovesForPlayer(gameModel.getCurrentPlayer().getColor())
                        .stream()
                        .filter(m -> m.getStartSquare() == selectedSquare)
                        .toList();
                highlightSelectedSquare(squarePanes[row][col]);
                highlightAvailableMoves();
            }
        } else {
            Optional<Move> chosenMoveOpt = availableMovesForSelectedPiece.stream()
                    .filter(m -> m.getEndSquare() == clickedBoardSquare)
                    .findFirst();

            if (chosenMoveOpt.isPresent()) {
                Move chosenMove = chosenMoveOpt.get();

                if (chosenMove.getPieceMoved().getType() == PieceType.PAWN &&
                        (chosenMove.getEndSquare().getRow() == 0 || chosenMove.getEndSquare().getRow() == Board.SIZE - 1)) {

                    PieceType promotionType = askForPromotionType();
                    if (promotionType == null) {
                        clearSelectionAndHighlights();
                        return;
                    }
                    final PieceType finalPromotionType = promotionType;
                    chosenMove = availableMovesForSelectedPiece.stream()
                            .filter(m -> m.getEndSquare() == clickedBoardSquare && m.isPromotion() && m.getPromotionPieceType() == finalPromotionType)
                            .findFirst()
                            .orElse(chosenMove);
                }

                boolean moveMade = gameModel.makeMove(chosenMove);
                if (moveMade) {
                    refreshBoardView();
                    updateTurnLabel();
                    updateStatusBasedOnGameState();
                    undoMoveButton.setDisable(gameModel.getMoveHistory().isEmpty());
                    // TODO: Update captured pieces view
                } else {
                    updateStatusLabel("Invalid move logic!");
                }
                clearSelectionAndHighlights();

            } else if (clickedBoardSquare.hasPiece() && clickedBoardSquare.getPiece().getColor() == gameModel.getCurrentPlayer().getColor()) {
                clearSelectionAndHighlights();
                selectedSquare = clickedBoardSquare;
                availableMovesForSelectedPiece = gameModel.getAllLegalMovesForPlayer(gameModel.getCurrentPlayer().getColor())
                        .stream()
                        .filter(m -> m.getStartSquare() == selectedSquare)
                        .toList();
                highlightSelectedSquare(squarePanes[row][col]);
                highlightAvailableMoves();
            } else {
                clearSelectionAndHighlights();
            }
        }
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
        if (selectedSquare != null) {
            removeHighlight(squarePanes[selectedSquare.getRow()][selectedSquare.getCol()]);
        }
        for (Move move : availableMovesForSelectedPiece) {
            removeHighlight(squarePanes[move.getEndSquare().getRow()][move.getEndSquare().getCol()]);
        }
        selectedSquare = null;
        availableMovesForSelectedPiece.clear();
    }

    private void highlightSelectedSquare(StackPane pane) {
        getHighlightBorderFromPane(pane).ifPresent(rect -> {
            rect.setStroke(Color.YELLOW);
            rect.setVisible(true);
        });
    }

    private void highlightAvailableMoves() {
        for (Move move : availableMovesForSelectedPiece) {
            StackPane targetPane = squarePanes[move.getEndSquare().getRow()][move.getEndSquare().getCol()];
            getHighlightBorderFromPane(targetPane).ifPresent(rect -> {
                if (gameModel.getBoard().getSquare(move.getEndSquare().getRow(), move.getEndSquare().getCol()).hasPiece()) {
                    rect.setStroke(Color.rgb(255, 0, 0, 0.7));
                } else {
                    rect.setStroke(Color.rgb(0, 255, 0, 0.5));
                }
                rect.setVisible(true);
            });
        }
    }

    private Optional<Rectangle> getHighlightBorderFromPane(StackPane pane) {
        if (!pane.getChildren().isEmpty() && pane.getChildren().get(0) instanceof Rectangle) {
            return Optional.of((Rectangle) pane.getChildren().get(0));
        }
        System.err.println("Highlight border rectangle not found in StackPane.");
        return Optional.empty();
    }

    private void removeHighlight(StackPane pane) {
        getHighlightBorderFromPane(pane).ifPresent(rect -> {
            rect.setStroke(Color.TRANSPARENT);
            rect.setVisible(false);
        });
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
            refreshBoardView();
            updateTurnLabel();
            updateStatusBasedOnGameState();
            undoMoveButton.setDisable(gameModel.getMoveHistory().isEmpty());
            // TODO: Update captured pieces view
        }
    }
}
