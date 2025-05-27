package org.group13.chessgame.utils;

import org.group13.chessgame.model.*;

public class NotationUtils {

    public static String squareToAlgebraic(Square square) {
        if (square == null) return "";
        char file = (char) ('a' + square.getCol());
        int rank = Board.SIZE - square.getRow();
        return "" + file + rank;
    }

    public static String moveToAlgebraic(Move move, Game game) {
        if (move == null || game == null) return "";

        // Castle
        if (move.isCastlingMove()) {
            String suffix = getCheckOrCheckmateSuffix(game);
            return (move.getEndSquare().getCol() < move.getStartSquare().getCol() ? "O-O-O" : "O-O") + suffix;
        }

        StringBuilder sb = new StringBuilder();
        Piece pieceMoved = move.getPieceMoved();

        // 1. Tên quân cờ
        if (pieceMoved.getType() != PieceType.PAWN) {
            sb.append(getPieceChar(pieceMoved.getType()));
        }

        // 2. TODO: Xử lý trường hợp có nhiều quân cờ cùng loại có thể đi đến ô đó
        // Trước mắt xử lý tốt (chốt), còn lại tính sau
        if (pieceMoved.getType() == PieceType.PAWN && move.getPieceCaptured() != null) {
            sb.append(squareToAlgebraic(move.getStartSquare()).charAt(0));
        }

        // 3. Ký hiệu ăn (x)
        if (move.getPieceCaptured() != null) {
            sb.append("x");
        }

        // 4. Ô đích
        sb.append(squareToAlgebraic(move.getEndSquare()));

        // 6. Phong cấp tốt
        if (move.isPromotion()) {
            sb.append("=").append(getPieceChar(move.getPromotionPieceType()));
        }

        // 7. Check (+) hoặc checkmate (#)
        sb.append(getCheckOrCheckmateSuffix(game));

        return sb.toString();
    }

    private static String getCheckOrCheckmateSuffix(Game game) {
        Game.GameState currentState = game.getGameState();

        if (currentState == Game.GameState.WHITE_WINS_CHECKMATE || currentState == Game.GameState.BLACK_WINS_CHECKMATE) {
            return "#";
        } else if (currentState == Game.GameState.CHECK) {
            if (game.isKingInCheck(game.getCurrentPlayer().getColor())) {
                return "+";
            }
        }
        if (currentState == Game.GameState.CHECK) return "+";

        return "";
    }

    private static String getPieceChar(PieceType type) {
        return switch (type) {
            case PAWN -> "";
            case ROOK -> "R";
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case QUEEN -> "Q";
            case KING -> "K";
        };
    }
}