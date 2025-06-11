package org.group13.chessgame.model;

import java.util.List;

public abstract class Piece {
    protected final PieceColor color;
    protected final PieceType type;
    protected boolean hasMoved;
    protected String imagePath;

    public Piece(PieceColor color, PieceType type) {
        this.color = color;
        this.type = type;
        this.hasMoved = false;
        setImagePath();
    }

    public abstract List<Move> getPseudoLegalMoves(Game game, int currentRow, int currentCol);

    public PieceColor getColor() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public String getImagePath() {
        return imagePath;
    }

    protected void setImagePath() {
        String colorStr = (color == PieceColor.WHITE) ? "w" : "b";
        String typeStr = pieceTypeToChar(this.type).toUpperCase();
        this.imagePath = String.format("/images/piece/%s%s.png", colorStr, typeStr);
    }

    public static String pieceTypeToChar(PieceType type) {
        return switch (type) {
            case PAWN -> "P";
            case ROOK -> "R";
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case QUEEN -> "Q";
            case KING -> "K";
            default -> "";
        };
    }

    public static PieceType charToPieceType(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'P' -> PieceType.PAWN;
            case 'R' -> PieceType.ROOK;
            case 'N' -> PieceType.KNIGHT;
            case 'B' -> PieceType.BISHOP;
            case 'Q' -> PieceType.QUEEN;
            case 'K' -> PieceType.KING;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return color.toString().charAt(0) + pieceTypeToChar(type);
    }
}
