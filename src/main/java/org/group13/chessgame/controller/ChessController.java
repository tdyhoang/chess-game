package org.group13.chessgame.controller;

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
import org.group13.chessgame.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChessController {
    private static final int BOARD_DISPLAY_SIZE = 600;
    private static final int SQUARE_SIZE = 75;
    private final List<Piece> whiteCapturedPieces = new ArrayList<>();
    private final List<Piece> blackCapturedPieces = new ArrayList<>();
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
    private Game gameModel;
    private StackPane[][] squarePanes;
    private Square selectedSquare = null;
    private List<Move> availableMovesForSelectedPiece = new ArrayList<>();

    @FXML
    public void initialize() {
        this.gameModel = new Game();
        this.squarePanes = new StackPane[Board.SIZE][Board.SIZE];
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

                Rectangle backgroundRect = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                if ((row + col) % 2 == 0) {
                    backgroundRect.setFill(Color.web("#f0d9b5"));
                } else {
                    backgroundRect.setFill(Color.web("#b58863"));
                }
                squarePane.getChildren().add(backgroundRect);

                ImageView pieceImageView = new ImageView();
                pieceImageView.setFitWidth(SQUARE_SIZE * 0.8);
                pieceImageView.setFitHeight(SQUARE_SIZE * 0.8);
                pieceImageView.setPreserveRatio(true);
                squarePane.getChildren().add(pieceImageView);

                javafx.scene.shape.Circle moveIndicator = new javafx.scene.shape.Circle(SQUARE_SIZE / 5.0);
                moveIndicator.setOpacity(0.5);
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

    private void performMove(Move move) {
        boolean moveMade = gameModel.makeMove(move);
        if (moveMade) {
            refreshBoardView();
            updateTurnLabel();
            updateStatusBasedOnGameState();
            undoMoveButton.setDisable(gameModel.getMoveHistory().isEmpty());
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
        if (selectedSquare != null) {
            removeHighlightStyling(squarePanes[selectedSquare.getRow()][selectedSquare.getCol()]);
        }
        for (Move move : availableMovesForSelectedPiece) {
            removeHighlightStyling(squarePanes[move.getEndSquare().getRow()][move.getEndSquare().getCol()]);
        }
        selectedSquare = null;
        availableMovesForSelectedPiece.clear();
    }

    private void highlightSelectedSquare(StackPane pane) {
        pane.setStyle("-fx-border-color: gold; -fx-border-width: 3;");
    }

    private void highlightAvailableMoves() {
        for (Move move : availableMovesForSelectedPiece) {
            StackPane targetPane = squarePanes[move.getEndSquare().getRow()][move.getEndSquare().getCol()];
            if (targetPane.getChildren().size() > 2 && targetPane.getChildren().get(2) instanceof javafx.scene.shape.Circle indicator) {
                if (move.getPieceCaptured() != null || move.isEnPassantMove()) {
                    indicator.setFill(Color.DARKRED);
                } else {
                    indicator.setFill(Color.DARKGREEN);
                }
                indicator.setVisible(true);
            }
        }
    }

    private void removeHighlightStyling(StackPane pane) {
        pane.setStyle("");
        if (pane.getChildren().size() > 2 && pane.getChildren().get(2) instanceof javafx.scene.shape.Circle indicator) {
            indicator.setVisible(false);
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
            imgView.setFitHeight(SQUARE_SIZE * 0.4);
            imgView.setPreserveRatio(true);
            capturedByWhiteArea.getChildren().add(imgView);
        }

        List<Piece> sortedBlackCaptures = new ArrayList<>(gameModel.getCapturedPieces(PieceColor.BLACK));
        for (Piece captured : sortedBlackCaptures) {
            ImageView imgView = new ImageView(new Image(getClass().getResourceAsStream(captured.getImagePath())));
            imgView.setFitHeight(SQUARE_SIZE * 0.4);
            imgView.setPreserveRatio(true);
            capturedByBlackArea.getChildren().add(imgView);
        }
    }
}
