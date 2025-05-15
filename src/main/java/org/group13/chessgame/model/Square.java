package org.group13.chessgame.model;

public class Square {
    private final int row;
    private final int col;
    private Piece piece;

    public Square(int row, int col) {
        this.row = row;
        this.col = col;
        this.piece = null;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public boolean isEmpty() {
        return this.piece == null;
    }

    public boolean hasPiece() {
        return this.piece != null;
    }

    public boolean hasEnemyPiece(PieceColor ownColor) {
        return hasPiece() && this.piece.getColor() != ownColor;
    }

    public boolean hasAllyPiece(PieceColor ownColor) {
        return hasPiece() && this.piece.getColor() == ownColor;
    }

    @Override
    public String toString() {
        return (char) ('a' + col) + "" + (8 - row);
    }
}
