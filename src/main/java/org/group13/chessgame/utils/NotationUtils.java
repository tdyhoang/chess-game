package org.group13.chessgame.utils;

import org.group13.chessgame.model.*;

public class NotationUtils {

    public static String squareToAlgebraic(Square square) {
        if (square == null) return "";
        char file = (char) ('a' + square.getCol());
        int rank = Board.SIZE - square.getRow();
        return "" + file + rank;
    }

    public static String moveToAlgebraic(Move move, Board board) {
        if (move == null) return "";

        // Castle
        if (move.isCastlingMove()) {
            if (move.getEndSquare().getCol() < move.getStartSquare().getCol()) {
                return "O-O-O";
            } else {
                return "O-O";
            }
        }

        StringBuilder sb = new StringBuilder();
        Piece pieceMoved = move.getPieceMoved();

        // 1. Tên quân cờ
        if (pieceMoved.getType() != PieceType.PAWN) {
            sb.append(getPieceChar(pieceMoved.getType()));
        }

        // 2. TODO: Xử lý trường hợp có nhiều quân cờ cùng loại có thể đi đến ô đó

        // 3. Tên quân cờ bị ăn
        if (pieceMoved.getType() == PieceType.PAWN && move.getPieceCaptured() != null) {
            sb.append(squareToAlgebraic(move.getStartSquare()).charAt(0));
        }

        // 4. Ký hiệu ăn (x)
        if (move.getPieceCaptured() != null) {
            sb.append("x");
        }

        // 5. Ô đích
        sb.append(squareToAlgebraic(move.getEndSquare()));

        // 6. Phong cấp tốt
        if (move.isPromotion()) {
            sb.append("=").append(getPieceChar(move.getPromotionPieceType()));
        }

        // 7. TODO: Check (+) hoặc checkmate (#)
//        if (game.isKingInCheck(game.getCurrentPlayer().getColor().opposite())) {
//            if (game.getGameState() == Game.GameState.WHITE_WINS_CHECKMATE || game.getGameState() == Game.GameState.BLACK_WINS_CHECKMATE) {
//                sb.append("#");
//            } else {
//                sb.append("+");
//            }
//        }

        return sb.toString();
    }

    private static String getPieceChar(PieceType type) {
        switch (type) {
            case PAWN:
                return "";
            case ROOK:
                return "R";
            case KNIGHT:
                return "N";
            case BISHOP:
                return "B";
            case QUEEN:
                return "Q";
            case KING:
                return "K";
            default:
                return "?";
        }
    }
}