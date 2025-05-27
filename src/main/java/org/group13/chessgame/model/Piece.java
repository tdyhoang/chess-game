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
        String typeStr = switch (type) {
            case PAWN -> "P";
            case ROOK -> "R";
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case QUEEN -> "Q";
            case KING -> "K";
        };
        this.imagePath = String.format("/images/piece/%s%s.png", colorStr, typeStr);
    }

    public abstract List<Move> getPseudoLegalMoves(Game game, int currentRow, int currentCol);

    @Override
    public String toString() {
        return color.toString().charAt(0) + type.toString().substring(0, 1);
    }
}
