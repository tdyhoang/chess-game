package org.group13.chessgame.model;

public class Player {
    private final PieceColor color;

    public Player(PieceColor color) {
        this.color = color;
    }

    public PieceColor getColor() {
        return color;
    }
}
