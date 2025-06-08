package org.group13.chessgame.controller;

import org.group13.chessgame.model.Move;

public record MovePairDisplay(int moveNumber, Move whiteMove, Move blackMove) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(moveNumber).append(". ");
        if (whiteMove != null && whiteMove.getStandardAlgebraicNotation() != null) {
            sb.append(whiteMove.getStandardAlgebraicNotation());
        } else if (whiteMove != null) {
            sb.append("???");
        } else if (blackMove != null) {
            sb.append("...");
        }

        if (blackMove != null && blackMove.getStandardAlgebraicNotation() != null) {
            sb.append(blackMove.getStandardAlgebraicNotation());
        } else if (blackMove != null) {
            sb.append("  ").append("???");
        }
        return sb.toString();
    }
}