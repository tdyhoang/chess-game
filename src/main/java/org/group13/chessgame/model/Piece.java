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
        String typeStr = "";
        switch (type) {
            case PAWN:
                typeStr = "P";
                break;
            case ROOK:
                typeStr = "R";
                break;
            case KNIGHT:
                typeStr = "N";
                break;
            case BISHOP:
                typeStr = "B";
                break;
            case QUEEN:
                typeStr = "Q";
                break;
            case KING:
                typeStr = "K";
                break;
        }
        this.imagePath = String.format("resources/images/pieces/%s%s.svg", colorStr, typeStr);
    }

    public abstract List<Move> getPseudoLegalMoves(Game game, int currentRow, int currentCol);

    @Override
    public String toString() {
        return color.toString().charAt(0) + type.toString().substring(0, 1);
    }
}
