package org.group13.chessgame.model;

import org.group13.chessgame.pgn.PgnHeaders;
import org.group13.chessgame.utils.NotationUtils;

import java.util.*;

public class Game {
    private static final ZobristTable zobristTable = new ZobristTable();
    private final Board board;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final Deque<Move> undoStack;
    private final Deque<Move> redoStack;
    private final List<Piece> piecesCapturedByWhite;
    private final List<Piece> piecesCapturedByBlack;
    // threefold repetition
    private final Map<Long, Integer> positionHistoryCount;
    private long currentPositionHash;
    // 50-move rule
    private int halfMoveClock;
    private Player currentPlayer;
    private GameState gameState;
    private Square whiteKingSquare;
    private Square blackKingSquare;
    private PgnHeaders pgnHeaders;

    public Game() {
        this.board = new Board();
        this.whitePlayer = new Player(PieceColor.WHITE);
        this.blackPlayer = new Player(PieceColor.BLACK);
        this.pgnHeaders = new PgnHeaders();
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.positionHistoryCount = new HashMap<>();
        this.piecesCapturedByWhite = new ArrayList<>();
        this.piecesCapturedByBlack = new ArrayList<>();
    }

    public void initializeGame() {
        board.initializeBoard();
        this.currentPlayer = whitePlayer;
        this.gameState = GameState.ACTIVE;
        this.pgnHeaders = new PgnHeaders();
        undoStack.clear();
        redoStack.clear();
        this.halfMoveClock = 0;
        this.currentPositionHash = calculateBoardHash();
        this.positionHistoryCount.clear();
        this.positionHistoryCount.put(this.currentPositionHash, 1);
        updateKingSquares();
        this.piecesCapturedByWhite.clear();
        this.piecesCapturedByBlack.clear();
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
        if (undoStack.isEmpty()) {
            return null;
        }
        return undoStack.peek();
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
        this.currentPositionHash = calculateBoardHash();
        this.positionHistoryCount.clear();
        this.positionHistoryCount.put(this.currentPositionHash, 1);
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


    public void surrender() {
        if (gameState != GameState.ACTIVE && gameState != GameState.CHECK) {
            return;
        }
        gameState = (currentPlayer.getColor() == PieceColor.WHITE) ? GameState.WHITE_SURRENDERS : GameState.BLACK_SURRENDERS;
        System.out.println("After surrender - GameState: " + gameState);
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

    public Move makeMove(Move moveFromUI) {
        Piece pieceToMoveFromUI = moveFromUI.getStartSquare().getPiece();
        if (pieceToMoveFromUI == null || pieceToMoveFromUI.getColor() != currentPlayer.getColor()) {
            System.err.println("Nước đi không hợp lệ: Không phải quân của bạn hoặc ô trống.");
            return null;
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
            return null;
        }

        actualMoveToMake.setHalfMoveClockBeforeMove(this.halfMoveClock);
        actualMoveToMake.setEnPassantTargetSquareBeforeMove(this.getEnPassantTargetSquare());

        long newHash = this.currentPositionHash;

        Square oldEnPassantTarget = actualMoveToMake.getEnPassantTargetSquareBeforeMove();
        if (oldEnPassantTarget != null) {
            newHash ^= zobristTable.getEnPassantFileKey(oldEnPassantTarget.getCol());
        }

        newHash ^= getCastlingHash();

        Piece pieceMoved = actualMoveToMake.getPieceMoved();
        newHash ^= zobristTable.getPieceKey(pieceMoved.getType(), pieceMoved.getColor(), actualMoveToMake.getStartSquare().getRow(), actualMoveToMake.getStartSquare().getCol());

        Piece capturedPiece = actualMoveToMake.getPieceCaptured();
        if (capturedPiece != null) {
            Square captureSquare;
            if (actualMoveToMake.isEnPassantMove()) {
                captureSquare = actualMoveToMake.getEnPassantCaptureSquare();
            } else {
                captureSquare = actualMoveToMake.getEndSquare();
            }
            newHash ^= zobristTable.getPieceKey(capturedPiece.getType(), capturedPiece.getColor(), captureSquare.getRow(), captureSquare.getCol());
        }

        if (actualMoveToMake.isCastlingMove()) {
            Piece rookForCastling = actualMoveToMake.getPieceOnRookStartForCastling();
            Square rookOriginalSquare = actualMoveToMake.getRookStartSquareForCastling();
            newHash ^= zobristTable.getPieceKey(rookForCastling.getType(), rookForCastling.getColor(), rookOriginalSquare.getRow(), rookOriginalSquare.getCol());
        }

        String sanBasic = NotationUtils.moveToAlgebraic(actualMoveToMake, this);

        board.applyMove(actualMoveToMake);

        Piece captured = actualMoveToMake.getPieceCaptured();
        if (captured != null) {
            if (captured.getColor() == PieceColor.BLACK) {
                piecesCapturedByWhite.add(captured);
            } else {
                piecesCapturedByBlack.add(captured);
            }
        }

        Piece pieceOnEndSquare = board.getPiece(actualMoveToMake.getEndSquare().getRow(), actualMoveToMake.getEndSquare().getCol());
        if (pieceOnEndSquare != null) {
            newHash ^= zobristTable.getPieceKey(pieceOnEndSquare.getType(), pieceOnEndSquare.getColor(), actualMoveToMake.getEndSquare().getRow(), actualMoveToMake.getEndSquare().getCol());
        }

        if (actualMoveToMake.isCastlingMove()) {
            Piece rookForCastling = actualMoveToMake.getPieceOnRookStartForCastling();
            Square rookNewSquare = actualMoveToMake.getRookEndSquareForCastling();
            newHash ^= zobristTable.getPieceKey(rookForCastling.getType(), rookForCastling.getColor(), rookNewSquare.getRow(), rookNewSquare.getCol());
        }

        newHash ^= getCastlingHash();

        Square newEnPassantTarget = null;
        assert pieceOnEndSquare != null;
        if (pieceOnEndSquare.getType() == PieceType.PAWN && Math.abs(actualMoveToMake.getStartSquare().getRow() - actualMoveToMake.getEndSquare().getRow()) == 2) {
            int direction = (pieceOnEndSquare.getColor() == PieceColor.WHITE) ? -1 : 1;
            newEnPassantTarget = board.getSquare(actualMoveToMake.getEndSquare().getRow() - direction, actualMoveToMake.getEndSquare().getCol());
        }
        if (newEnPassantTarget != null) {
            newHash ^= zobristTable.getEnPassantFileKey(newEnPassantTarget.getCol());
        }

        newHash ^= zobristTable.getBlackToMoveKey();

        this.currentPositionHash = newHash;

        if (actualMoveToMake.getPieceMoved().getType() == PieceType.KING) {
            if (actualMoveToMake.getPieceMoved().getColor() == PieceColor.WHITE) {
                whiteKingSquare = actualMoveToMake.getEndSquare();
            } else {
                blackKingSquare = actualMoveToMake.getEndSquare();
            }
        }

        if (actualMoveToMake.getPieceMoved().getType() == PieceType.PAWN || actualMoveToMake.getPieceCaptured() != null) {
            this.resetHalfMoveClock();
        } else {
            this.incrementHalfMoveClock();
        }

        actualMoveToMake.setHashGenerated(this.currentPositionHash);
        undoStack.push(actualMoveToMake);
        redoStack.clear();

        int count = this.positionHistoryCount.getOrDefault(this.currentPositionHash, 0) + 1;
        this.positionHistoryCount.put(this.currentPositionHash, count);

        switchPlayer();
        updateKingSquares();

        updateGameState();

        String suffix = "";
        if (this.gameState == GameState.WHITE_WINS_CHECKMATE || this.gameState == GameState.BLACK_WINS_CHECKMATE) {
            suffix = "#";
        } else if (this.gameState == GameState.CHECK) {
            suffix = "+";
        }
        actualMoveToMake.setStandardAlgebraicNotation(sanBasic + suffix);

        return actualMoveToMake;
    }

    private long getCastlingHash() {
        long castlingHash = 0;
        if (canCastleKingside(PieceColor.WHITE)) castlingHash ^= zobristTable.getCastlingRightsKey(0);
        if (canCastleQueenside(PieceColor.WHITE)) castlingHash ^= zobristTable.getCastlingRightsKey(1);
        if (canCastleKingside(PieceColor.BLACK)) castlingHash ^= zobristTable.getCastlingRightsKey(2);
        if (canCastleQueenside(PieceColor.BLACK)) castlingHash ^= zobristTable.getCastlingRightsKey(3);
        return castlingHash;
    }

    public void switchPlayer() {
        currentPlayer = (currentPlayer == whitePlayer) ? blackPlayer : whitePlayer;
    }

    private void updateGameState() {
        if (gameState == GameState.WHITE_SURRENDERS || gameState == GameState.BLACK_SURRENDERS) {
            return;
        }
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
            if (isThreefoldRepetition()) {
                gameState = GameState.THREEFOLD_REPETITION_DRAW;
            }
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
        if (gameState == GameState.BLACK_WINS_CHECKMATE || gameState == GameState.WHITE_WINS_CHECKMATE || gameState == GameState.STALEMATE_DRAW || gameState == GameState.FIFTY_MOVE_DRAW || gameState == GameState.THREEFOLD_REPETITION_DRAW || gameState == GameState.INSUFFICIENT_MATERIAL_DRAW || gameState == GameState.BLACK_SURRENDERS || gameState == GameState.WHITE_SURRENDERS) {
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
        PieceColor bishopColorSquare = null;

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null) {
                    if (p.getColor() == PieceColor.WHITE) whitePieces.add(p);
                    else blackPieces.add(p);

                    if (p.getType() == PieceType.BISHOP) {
                        PieceColor currentBishopColorSquare = ((r + c) % 2 == 0) ? PieceColor.WHITE : PieceColor.BLACK;
                        if (bishopColorSquare == null) {
                            bishopColorSquare = currentBishopColorSquare;
                        } else {
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

    // private void addCurrentPositionToHistory() {}
    private boolean isThreefoldRepetition() {
        if (this.currentPositionHash == 0 && !this.positionHistoryCount.isEmpty()) {
            System.err.println("Warning: Checking threefold repetition with uninitialized/zero currentPositionHash.");
            return false;
        }
        return this.positionHistoryCount.getOrDefault(this.currentPositionHash, 0) >= 3;
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

    public Move undo() {
        if (undoStack.isEmpty()) {
            return null;
        }
        Move moveToUndo = undoStack.pop();
        redoStack.push(moveToUndo);

        long hashThatWasGenerated = this.currentPositionHash;
        int oldCount = this.positionHistoryCount.getOrDefault(hashThatWasGenerated, 0);
        if (oldCount > 0) {
            this.positionHistoryCount.put(hashThatWasGenerated, oldCount - 1);
            if (this.positionHistoryCount.get(hashThatWasGenerated) == 0) {
                this.positionHistoryCount.remove(hashThatWasGenerated);
            }
        }

        board.undoMove(moveToUndo);

        this.halfMoveClock = moveToUndo.getHalfMoveClockBeforeMove();

        switchPlayer();
        updateKingSquares();

        this.currentPositionHash = calculateBoardHash();

        updateGameState();
        return moveToUndo;
    }

    public Move redo() {
        if (redoStack.isEmpty()) {
            return null;
        }
        Move moveToRedo = redoStack.pop();

        board.applyMove(moveToRedo);

        Piece pieceThatMoved = moveToRedo.getPieceMoved();

        if (pieceThatMoved.getType() == PieceType.PAWN || moveToRedo.getPieceCaptured() != null) {
            this.resetHalfMoveClock();
        } else {
            this.incrementHalfMoveClock();
        }

        undoStack.push(moveToRedo);

        switchPlayer();
        updateKingSquares();

        this.currentPositionHash = calculateBoardHash();

        int count = this.positionHistoryCount.getOrDefault(this.currentPositionHash, 0) + 1;
        this.positionHistoryCount.put(this.currentPositionHash, count);

        updateGameState();

        return moveToRedo;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public List<Move> getPlayedMoveSequence() {
        List<Move> sequence = new ArrayList<>(undoStack);
        java.util.Collections.reverse(sequence);
        sequence.addAll(redoStack);
        return sequence;
    }

    public void resetHalfMoveClock() {
        this.halfMoveClock = 0;
    }

    private long calculateBoardHash() {
        long hash = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null) {
                    hash ^= zobristTable.getPieceKey(p.getType(), p.getColor(), r, c);
                }
            }
        }

        if (currentPlayer.getColor() == PieceColor.BLACK) {
            hash ^= zobristTable.getBlackToMoveKey();
        }

        hash ^= getCastlingHash();

        Square epTarget = getEnPassantTargetSquare();
        if (epTarget != null) {
            hash ^= zobristTable.getEnPassantFileKey(epTarget.getCol());
        }
        return hash;
    }

    private boolean canCastleKingside(PieceColor color) {
        Square kingSquare = getKingSquare(color);
        if (kingSquare == null || kingSquare.getPiece() == null || kingSquare.getPiece().hasMoved()) return false;
        int kingRow = kingSquare.getRow();
        Square rookSquare = board.getSquare(kingRow, Board.SIZE - 1);
        return rookSquare.hasPiece() && rookSquare.getPiece().getType() == PieceType.ROOK && rookSquare.getPiece().getColor() == color && !rookSquare.getPiece().hasMoved();
    }

    private boolean canCastleQueenside(PieceColor color) {
        Square kingSquare = getKingSquare(color);
        if (kingSquare == null || kingSquare.getPiece() == null || kingSquare.getPiece().hasMoved()) return false;
        int kingRow = kingSquare.getRow();
        Square rookSquare = board.getSquare(kingRow, 0);
        return rookSquare.hasPiece() && rookSquare.getPiece().getType() == PieceType.ROOK && rookSquare.getPiece().getColor() == color && !rookSquare.getPiece().hasMoved();
    }

    public void incrementHalfMoveClock() {
        this.halfMoveClock++;
    }

    public int getHalfMoveClock() {
        return this.halfMoveClock;
    }

    public void _test_triggerUpdateGameState() {
        this.updateGameState();
    }

    public Square getEnPassantTargetSquare() {
        Move lastMove = getLastMove();
        if (lastMove != null && lastMove.getPieceMoved().getType() == PieceType.PAWN && Math.abs(lastMove.getStartSquare().getRow() - lastMove.getEndSquare().getRow()) == 2) {
            int direction = (lastMove.getPieceMoved().getColor() == PieceColor.WHITE) ? -1 : 1;
            return board.getSquare(lastMove.getEndSquare().getRow() - direction, lastMove.getEndSquare().getCol());
        }
        return null;
    }

    public long getCurrentPositionHash() {
        return this.currentPositionHash;
    }

    public Map<Long, Integer> getPositionHistoryCount() {
        // return Collections.unmodifiableMap(this.positionHistoryCount);
        return this.positionHistoryCount;
    }

    public List<Piece> getCapturedPieces(PieceColor capturerColor) {
        return (capturerColor == PieceColor.WHITE) ? piecesCapturedByWhite : piecesCapturedByBlack;
    }

    public boolean makeMoveFromSquares(Square fromSquareModel, Square toSquareModel, PieceType promotionTypeModel) {
        List<Move> legalMoves = getAllLegalMovesForPlayer(this.currentPlayer.getColor());
        Move matchedMove = null;

        for (Move legalMove : legalMoves) {
            if (legalMove.getStartSquare() == fromSquareModel && legalMove.getEndSquare() == toSquareModel) {

                if (legalMove.isPromotion()) {
                    if (legalMove.getPromotionPieceType() == promotionTypeModel) {
                        matchedMove = legalMove;
                        break;
                    }
                } else {
                    if (promotionTypeModel == null) {
                        matchedMove = legalMove;
                        break;
                    }
                }
            }
        }

        if (matchedMove != null) {
            return makeMove(matchedMove) != null;
        } else {
            System.err.println("PgnParser Error: Could not find a legal move in model from " + NotationUtils.squareToAlgebraic(fromSquareModel) + " to " + NotationUtils.squareToAlgebraic(toSquareModel) + (promotionTypeModel != null ? "=" + promotionTypeModel : "") + " for player " + currentPlayer.getColor() + ". Current FEN (model): " + getBoard().getFen());
            System.err.println("Legal moves available in model for " + currentPlayer.getColor() + ":");
            for (Move m : legalMoves) {
                String san = NotationUtils.moveToAlgebraic(m, this);
                System.err.println("  " + san + " (raw: " + m.getStartSquare() + "->" + m.getEndSquare() + (m.isPromotion() ? "=" + m.getPromotionPieceType() : "") + ")");
            }
            return false;
        }
    }

    public String getFen() {
        StringBuilder fenBuilder = new StringBuilder();
        fenBuilder.append(this.board.getFen());

        fenBuilder.append(" ").append(this.currentPlayer.getColor() == PieceColor.WHITE ? "w" : "b");

        fenBuilder.append(" ");
        StringBuilder castlingFen = new StringBuilder();
        if (canCastleKingside(PieceColor.WHITE)) castlingFen.append("K");
        if (canCastleQueenside(PieceColor.WHITE)) castlingFen.append("Q");
        if (canCastleKingside(PieceColor.BLACK)) castlingFen.append("k");
        if (canCastleQueenside(PieceColor.BLACK)) castlingFen.append("q");
        if (castlingFen.isEmpty()) castlingFen.append("-");
        fenBuilder.append(castlingFen);

        fenBuilder.append(" ");
        Square epTarget = getEnPassantTargetSquare();
        if (epTarget != null) {
            fenBuilder.append(NotationUtils.squareToAlgebraic(epTarget));
        } else {
            fenBuilder.append("-");
        }

        fenBuilder.append(" ").append(this.halfMoveClock);

        int fullMoves = (undoStack.size() / 2) + 1;
        fenBuilder.append(" ").append(fullMoves);

        return fenBuilder.toString();
    }

    public PgnHeaders getPgnHeaders() {
        return pgnHeaders;
    }

    public void setPgnHeaders(PgnHeaders headers) {
        this.pgnHeaders = (headers != null) ? headers : new PgnHeaders();
    }

    public enum GameState {
        ACTIVE, CHECK, WHITE_WINS_CHECKMATE, BLACK_WINS_CHECKMATE, STALEMATE_DRAW, FIFTY_MOVE_DRAW, THREEFOLD_REPETITION_DRAW, INSUFFICIENT_MATERIAL_DRAW, WHITE_SURRENDERS, BLACK_SURRENDERS
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