package org.group13.chessgame.utils;

import org.group13.chessgame.model.*;

import java.util.ArrayList;
import java.util.List;

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
            return (move.getEndSquare().getCol() < move.getStartSquare().getCol() ? "O-O-O" : "O-O");
        }

        StringBuilder sb = new StringBuilder();
        Piece pieceMoved = move.getPieceMoved();

        // 1. Tên quân cờ
        if (pieceMoved.getType() != PieceType.PAWN) {
            sb.append(getPieceChar(pieceMoved.getType()));
        }

        // 2. Xử lý trường hợp có nhiều quân cờ cùng loại có thể đi đến ô đó
        String disambiguation = calculateDisambiguation(move, game);
        sb.append(disambiguation);

        // 3. Ký hiệu ăn (x)
        if (move.getPieceCaptured() != null) {
            if (pieceMoved.getType() == PieceType.PAWN && disambiguation.isEmpty()) {
                sb.append(squareToAlgebraic(move.getStartSquare()).charAt(0));
            }
            sb.append("x");
        }

        // 4. Ô đích
        sb.append(squareToAlgebraic(move.getEndSquare()));

        // 6. Phong cấp tốt
        if (move.isPromotion()) {
            sb.append("=").append(getPieceChar(move.getPromotionPieceType()));
        }

        return sb.toString();
    }

    private static String calculateDisambiguation(Move move, Game game) {
        Piece pieceMoved = move.getPieceMoved();
        Square fromSquare = move.getStartSquare();
        Square toSquare = move.getEndSquare();

        if (pieceMoved.getType() == PieceType.PAWN || pieceMoved.getType() == PieceType.KING) return "";

        List<Square> ambiguousSources = new ArrayList<>();
        List<Move> allLegalMovesForPlayer = game.getAllLegalMovesForPlayer(pieceMoved.getColor());
        for (Move legalMove : allLegalMovesForPlayer) {
            if (legalMove.getStartSquare() != fromSquare && legalMove.getPieceMoved().getType() == pieceMoved.getType() && legalMove.getEndSquare() == toSquare) {
                ambiguousSources.add(legalMove.getStartSquare());
            }
        }

        if (ambiguousSources.isEmpty()) return "";

        boolean fileSufficient = true;
        for (Square otherSq : ambiguousSources) {
            if (otherSq.getCol() == fromSquare.getCol()) {
                fileSufficient = false;
                break;
            }
        }
        if (fileSufficient) {
            return String.valueOf(squareToAlgebraic(fromSquare).charAt(0));
        }

        boolean rankSufficient = true;
        for (Square otherSq : ambiguousSources) {
            if (otherSq.getRow() == fromSquare.getRow()) {
                rankSufficient = false;
                break;
            }
        }
        if (rankSufficient) {
            return String.valueOf(squareToAlgebraic(fromSquare).charAt(1));
        }

        return squareToAlgebraic(fromSquare);
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