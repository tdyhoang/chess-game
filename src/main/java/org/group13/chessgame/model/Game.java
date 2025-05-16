package org.group13.chessgame.model;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private final Board board;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final List<Move> moveHistory;
    // threefold repetition
    private final List<Long> positionHistoryHash;
    private GameState previousGameState;
    private Player currentPlayer;
    private GameState gameState;
    private Square whiteKingSquare;
    private Square blackKingSquare;
    // 50-move rule
    private int halfMoveClock;

    public Game() {
        this.board = new Board();
        this.whitePlayer = new Player(PieceColor.WHITE);
        this.blackPlayer = new Player(PieceColor.BLACK);
        this.moveHistory = new ArrayList<>();
        this.positionHistoryHash = new ArrayList<>();
    }

    public void initializeGame() {
        board.initializeBoard();
        this.currentPlayer = whitePlayer;
        this.gameState = GameState.ACTIVE;
        this.moveHistory.clear();
        this.halfMoveClock = 0;
        this.positionHistoryHash.clear();
        updateKingSquares();
        // addCurrentPositionToHistory();
    }

    public Board getBoard() {
        return board;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Move getLastMove() {
        if (moveHistory.isEmpty()) {
            return null;
        }
        return moveHistory.get(moveHistory.size() - 1);
    }

    public List<Move> getMoveHistory() {
        return moveHistory;
    }

    public void setupBoardForTest(List<PiecePlacement> placements, PieceColor playerWhoseTurnItIs) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                board.setPiece(r, c, null);
            }
        }
        if (placements != null) {
            for (PiecePlacement p : placements) {
                board.setPiece(p.row, p.col, p.piece);
                if (p.piece != null) {
                    p.piece.setHasMoved(p.piece.hasMoved());
                }
            }
        }
        updateKingSquares();
        setCurrentPlayerColorForTest(playerWhoseTurnItIs);
        // updateGameState();
    }

    public void setCurrentPlayerColorForTest(PieceColor color) {
        if (color == PieceColor.WHITE) {
            this.currentPlayer = whitePlayer;
        } else {
            this.currentPlayer = blackPlayer;
        }
    }

    public Player getWhitePlayerInstance() {
        return whitePlayer;
    }

    public Player getBlackPlayerInstance() {
        return blackPlayer;
    }

    private void updateKingSquares() {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Square s = board.getSquare(r, c);
                if (s.hasPiece()) {
                    Piece p = s.getPiece();
                    if (p.getType() == PieceType.KING) {
                        if (p.getColor() == PieceColor.WHITE) {
                            whiteKingSquare = s;
                        } else {
                            blackKingSquare = s;
                        }
                    }
                }
            }
        }
    }

    public Square getKingSquare(PieceColor kingColor) {
        return (kingColor == PieceColor.WHITE) ? whiteKingSquare : blackKingSquare;
    }

    public boolean makeMove(Move moveFromUI) {
        Piece pieceToMoveFromUI = moveFromUI.getStartSquare().getPiece();
        if (pieceToMoveFromUI == null || pieceToMoveFromUI.getColor() != currentPlayer.getColor()) {
            System.err.println("Nước đi không hợp lệ: Không phải quân của bạn hoặc ô trống.");
            return false;
        }

        List<Move> legalMoves = getAllLegalMovesForPlayer(currentPlayer.getColor());
        Move actualMoveToMake = null;

        for (Move legalMv : legalMoves) {
            if (legalMv.getStartSquare() == moveFromUI.getStartSquare() && legalMv.getEndSquare() == moveFromUI.getEndSquare() && legalMv.getPromotionPieceType() == moveFromUI.getPromotionPieceType()) {

                if (legalMv.isCastlingMove() && pieceToMoveFromUI.getType() == PieceType.KING && Math.abs(legalMv.getEndSquare().getCol() - legalMv.getStartSquare().getCol()) == 2) {
                    actualMoveToMake = legalMv;
                    break;
                }
                if (!legalMv.isCastlingMove()) {
                    actualMoveToMake = legalMv;
                    break;
                }
            }
        }

        if (actualMoveToMake == null) {
            System.err.println("Nước đi không hợp lệ: " + moveFromUI + " không có trong danh sách nước đi hợp lệ hoặc thông tin không khớp.");
            System.err.println("Legal moves for " + currentPlayer.getColor() + ":");
            for (Move m : legalMoves) System.err.println("  " + m.toString());
            return false;
        }

        board.applyMove(actualMoveToMake);

        if (actualMoveToMake.getPieceMoved().getType() == PieceType.KING) {
            if (actualMoveToMake.getPieceMoved().getColor() == PieceColor.WHITE) {
                whiteKingSquare = actualMoveToMake.getEndSquare();
            } else {
                blackKingSquare = actualMoveToMake.getEndSquare();
            }
        }

        moveHistory.add(actualMoveToMake);

        if (actualMoveToMake.getPieceMoved().getType() == PieceType.PAWN || actualMoveToMake.getPieceCaptured() != null) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        // addCurrentPositionToHistory();

        switchPlayer();

        updateGameState();

        return true;
    }

    public void switchPlayer() {
        currentPlayer = (currentPlayer == whitePlayer) ? blackPlayer : whitePlayer;
    }

    private void updateGameState() {
        PieceColor opponentColor = currentPlayer.getColor();
        boolean inCheck = isKingInCheck(opponentColor);
        List<Move> legalMovesForCurrentPlayer = getAllLegalMovesForPlayer(opponentColor);

        if (inCheck) {
            if (legalMovesForCurrentPlayer.isEmpty()) {
                gameState = (opponentColor == PieceColor.WHITE) ? GameState.BLACK_WINS_CHECKMATE : GameState.WHITE_WINS_CHECKMATE;
            } else {
                gameState = GameState.CHECK;
            }
        } else {
            if (legalMovesForCurrentPlayer.isEmpty()) {
                gameState = GameState.STALEMATE_DRAW;
            } else {
                gameState = GameState.ACTIVE;
            }
        }

        if (gameState == GameState.ACTIVE || gameState == GameState.CHECK) {
            if (isFiftyMoveRule()) {
                gameState = GameState.FIFTY_MOVE_DRAW;
            }
            // if (isThreefoldRepetition()) {
            //     gameState = GameState.THREEFOLD_REPETITION_DRAW;
            // }
            if (isInsufficientMaterial()) {
                gameState = GameState.INSUFFICIENT_MATERIAL_DRAW;
            }
        }
        System.out.println("New GameState: " + gameState + (inCheck && gameState != GameState.BLACK_WINS_CHECKMATE && gameState != GameState.WHITE_WINS_CHECKMATE ? " (King " + opponentColor + " is in CHECK!)" : ""));
    }

    public boolean isKingInCheck(PieceColor kingColor) {
        Square kingSq = getKingSquare(kingColor);
        if (kingSq == null) {
            System.err.println("Không tìm thấy Vua " + kingColor);
            return false;
        }
        return isSquareAttackedBy(kingSq, kingColor.opposite());
    }

    public boolean isSquareAttackedBy(Square targetSquare, PieceColor attackerColor) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Square s = board.getSquare(r, c);
                if (s.hasPiece() && s.getPiece().getColor() == attackerColor) {
                    Piece attacker = s.getPiece();
                    if (attacker.getType() == PieceType.PAWN) {
                        int direction = (attacker.getColor() == PieceColor.WHITE) ? -1 : 1;
                        int attackRow = s.getRow() + direction;
                        if (attackRow == targetSquare.getRow()) {
                            if (s.getCol() - 1 == targetSquare.getCol() || s.getCol() + 1 == targetSquare.getCol()) {
                                return true;
                            }
                        }
                    } else {
                        List<Move> pseudoMoves = attacker.getPseudoLegalMoves(this, r, c);
                        for (Move pseudoMove : pseudoMoves) {
                            if (pseudoMove.getEndSquare() == targetSquare) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<Move> getAllLegalMovesForPlayer(PieceColor playerColor) {
        List<Move> legalMoves = new ArrayList<>();
        if (gameState == GameState.BLACK_WINS_CHECKMATE || gameState == GameState.WHITE_WINS_CHECKMATE || gameState == GameState.STALEMATE_DRAW || gameState == GameState.FIFTY_MOVE_DRAW || gameState == GameState.THREEFOLD_REPETITION_DRAW || gameState == GameState.INSUFFICIENT_MATERIAL_DRAW) {
            return legalMoves;
        }

        Square originalKingSquare = getKingSquare(playerColor);

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Square s = board.getSquare(r, c);
                if (s.hasPiece() && s.getPiece().getColor() == playerColor) {
                    Piece piece = s.getPiece();
                    List<Move> pseudoMoves = piece.getPseudoLegalMoves(this, r, c);

                    for (Move pseudoMove : pseudoMoves) {
                        // Piece pieceToMove = s.getPiece();
                        // boolean originalHasMovedStatus = pieceToMove.hasMoved();
                        // Piece originalCaptured = pseudoMove.getEndSquare().getPiece();

                        board.applyMove(pseudoMove);

                        if (piece.getType() == PieceType.KING) {
                            if (playerColor == PieceColor.WHITE) {
                                whiteKingSquare = pseudoMove.getEndSquare();
                            } else {
                                blackKingSquare = pseudoMove.getEndSquare();
                            }
                        }

                        if (!isKingInCheck(playerColor)) {
                            legalMoves.add(pseudoMove);
                        }
                        board.undoMove(pseudoMove);

                        if (piece.getType() == PieceType.KING) {
                            if (playerColor == PieceColor.WHITE) {
                                whiteKingSquare = originalKingSquare;
                            } else {
                                blackKingSquare = originalKingSquare;
                            }
                        }

                        // if (originalCapturedPiece != null) {
                        //    pseudoMove.getEndSquare().setPiece(originalCapturedPiece);
                        // } else if (!pseudoMove.isEnPassantMove()) {
                        //    pseudoMove.getEndSquare().setPiece(null);
                        // }
                    }
                }
            }
        }

        if (playerColor == PieceColor.WHITE) {
            whiteKingSquare = originalKingSquare;
        } else {
            blackKingSquare = originalKingSquare;
        }

        addCastlingMoves(legalMoves, playerColor);

        return legalMoves;
    }

    private void addCastlingMoves(List<Move> legalMoves, PieceColor playerColor) {
        Square kingSquare = getKingSquare(playerColor);
        if (kingSquare == null || kingSquare.getPiece() == null || kingSquare.getPiece().hasMoved() || isKingInCheck(playerColor)) {
            return;
        }

        int kingRow = kingSquare.getRow();

        Square kingsideRookSquare = board.getSquare(kingRow, Board.SIZE - 1);
        if (kingsideRookSquare.hasPiece() && kingsideRookSquare.getPiece().getType() == PieceType.ROOK && !kingsideRookSquare.getPiece().hasMoved()) {
            if (board.getSquare(kingRow, 5).isEmpty() && board.getSquare(kingRow, 6).isEmpty() && !isSquareAttackedBy(board.getSquare(kingRow, 5), playerColor.opposite()) && !isSquareAttackedBy(board.getSquare(kingRow, 6), playerColor.opposite())) {
                Piece king = kingSquare.getPiece();
                Piece rook = kingsideRookSquare.getPiece();

                Square kingEndSquare = board.getSquare(kingRow, 6);
                Square rookEndSquare = board.getSquare(kingRow, 5);
                Move castlingMove = new Move(kingSquare, kingEndSquare, king, king.hasMoved());
                castlingMove.setCastlingMove(true);
                castlingMove.setRookStartSquareForCastling(kingsideRookSquare);
                castlingMove.setRookEndSquareForCastling(rookEndSquare);
                castlingMove.setRookInfoForCastlingUndo(rook, rook.hasMoved());
                legalMoves.add(castlingMove);
            }
        }

        Square queensideRookSquare = board.getSquare(kingRow, 0);
        if (queensideRookSquare.hasPiece() && queensideRookSquare.getPiece().getType() == PieceType.ROOK && !queensideRookSquare.getPiece().hasMoved()) {
            if (board.getSquare(kingRow, 1).isEmpty() && board.getSquare(kingRow, 2).isEmpty() && board.getSquare(kingRow, 3).isEmpty() && !isSquareAttackedBy(board.getSquare(kingRow, 3), playerColor.opposite()) && !isSquareAttackedBy(board.getSquare(kingRow, 2), playerColor.opposite())) {
                Piece king = kingSquare.getPiece();
                Piece rook = queensideRookSquare.getPiece();

                Square kingEndSquare = board.getSquare(kingRow, 2);
                Square rookEndSquare = board.getSquare(kingRow, 3);
                Move castlingMove = new Move(kingSquare, kingEndSquare, king, king.hasMoved());
                castlingMove.setCastlingMove(true);
                castlingMove.setRookStartSquareForCastling(queensideRookSquare);
                castlingMove.setRookEndSquareForCastling(rookEndSquare);
                castlingMove.setRookInfoForCastlingUndo(rook, rook.hasMoved());
                legalMoves.add(castlingMove);
            }
        }
    }

    private boolean isFiftyMoveRule() {
        return halfMoveClock >= 100;
    }

    private boolean isInsufficientMaterial() {
        List<Piece> whitePieces = new ArrayList<>();
        List<Piece> blackPieces = new ArrayList<>();
        int knightCount = 0;
        int bishopCount = 0;
        PieceColor bishopColorSquare = null;

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null) {
                    if (p.getColor() == PieceColor.WHITE) whitePieces.add(p);
                    else blackPieces.add(p);

                    if (p.getType() == PieceType.KNIGHT) knightCount++;
                    if (p.getType() == PieceType.BISHOP) {
                        bishopCount++;
                        if (bishopColorSquare == null) {
                            bishopColorSquare = ((r + c) % 2 == 0) ? PieceColor.WHITE : PieceColor.BLACK;
                        } else {
                            PieceColor currentBishopColorSquare = ((r + c) % 2 == 0) ? PieceColor.WHITE : PieceColor.BLACK;
                            if (bishopColorSquare == currentBishopColorSquare && (p.getType() == PieceType.BISHOP && countPiecesOnBoard(PieceType.BISHOP) == 2)) {
                            } else if (bishopColorSquare != currentBishopColorSquare) {
                                bishopColorSquare = null;
                            }
                        }
                    }
                    if (p.getType() == PieceType.PAWN || p.getType() == PieceType.ROOK || p.getType() == PieceType.QUEEN) {
                        return false;
                    }
                }
            }
        }

        if (whitePieces.size() == 1 && blackPieces.size() == 1) return true;

        if ((whitePieces.size() == 1 && blackPieces.size() == 2 && (containsPieceType(blackPieces, PieceType.KNIGHT) || containsPieceType(blackPieces, PieceType.BISHOP))) || (blackPieces.size() == 1 && whitePieces.size() == 2 && (containsPieceType(whitePieces, PieceType.KNIGHT) || containsPieceType(whitePieces, PieceType.BISHOP)))) {
            return true;
        }

        if (whitePieces.size() == 2 && containsPieceType(whitePieces, PieceType.BISHOP) && blackPieces.size() == 2 && containsPieceType(blackPieces, PieceType.BISHOP)) {

            Square whiteBishopSquare = findPieceSquare(PieceColor.WHITE, PieceType.BISHOP);
            Square blackBishopSquare = findPieceSquare(PieceColor.BLACK, PieceType.BISHOP);

            if (whiteBishopSquare != null && blackBishopSquare != null) {
                boolean whiteBishopOnWhiteSquare = (whiteBishopSquare.getRow() + whiteBishopSquare.getCol()) % 2 == 0;
                boolean blackBishopOnWhiteSquare = (blackBishopSquare.getRow() + blackBishopSquare.getCol()) % 2 == 0;
                return whiteBishopOnWhiteSquare == blackBishopOnWhiteSquare;
            }
        }


        return false;
    }

    private boolean containsPieceType(List<Piece> pieces, PieceType type) {
        for (Piece p : pieces) {
            if (p.getType() == type) return true;
        }
        return false;
    }

    // TODO: Implement Zobrist Hashing for threefold repetition
    // private void addCurrentPositionToHistory() {}
    // private boolean isThreefoldRepetition() {}

    private int countPieceType(List<Piece> pieces, PieceType type) {
        int count = 0;
        for (Piece p : pieces) {
            if (p.getType() == type) count++;
        }
        return count;
    }

    private int countPiecesOnBoard(PieceType type) {
        int count = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getType() == type) {
                    count++;
                }
            }
        }
        return count;
    }

    private Square findPieceSquare(PieceColor color, PieceType type) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getColor() == color && p.getType() == type) {
                    return board.getSquare(r, c);
                }
            }
        }
        return null;
    }

    public boolean undoLastMove() {
        if (moveHistory.isEmpty()) {
            return false;
        }
        Move lastMove = moveHistory.remove(moveHistory.size() - 1);

        board.undoMove(lastMove);

        updateKingSquares();

        switchPlayer();

        if (lastMove.getPieceMoved().getType() == PieceType.PAWN || lastMove.getPieceCaptured() != null) {
        } else {
            if (halfMoveClock > 0) halfMoveClock--;
        }


        // removeLastPositionFromHistory();
        // currentPositionHash = calculateCurrentBoardHash();

        updateGameState();

        return true;
    }

    public void resetHalfMoveClock() {
        this.halfMoveClock = 0;
    }

    // public long getBoardHash() {}

    public void incrementHalfMoveClock() {
        this.halfMoveClock++;
    }

    public int getHalfMoveClock() {
        return this.halfMoveClock;
    }

    public void _test_triggerUpdateGameState() {
        this.updateGameState();
    }

    public enum GameState {
        ACTIVE, CHECK, WHITE_WINS_CHECKMATE, BLACK_WINS_CHECKMATE, STALEMATE_DRAW, FIFTY_MOVE_DRAW, THREEFOLD_REPETITION_DRAW, INSUFFICIENT_MATERIAL_DRAW
    }

    public static class PiecePlacement {
        public final int row;
        public final int col;
        public final Piece piece;
        public final boolean hasMoved;

        public PiecePlacement(int row, int col, Piece piece) {
            this(row, col, piece, piece != null && piece.hasMoved());
        }

        public PiecePlacement(int row, int col, Piece piece, boolean hasMoved) {
            this.row = row;
            this.col = col;
            this.piece = piece;
            this.hasMoved = hasMoved;
        }
    }
}