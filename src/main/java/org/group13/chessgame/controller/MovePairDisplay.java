package org.group13.chessgame.controller;

import org.group13.chessgame.model.Move;

public class MovePairDisplay {
    private final int moveNumber;
    private final Move whiteMove;
    private final Move blackMove;

    public MovePairDisplay(int moveNumber, Move whiteMove, Move blackMove) {
        this.moveNumber = moveNumber;
        this.whiteMove = whiteMove;
        this.blackMove = blackMove;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public Move getWhiteMove() {
        return whiteMove;
    }

    public Move getBlackMove() {
        return blackMove;
    }

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
            sb.append("  ").append(blackMove.getStandardAlgebraicNotation());
        } else if (blackMove != null) {
            sb.append("  ").append("???");
        }
        return sb.toString();
    }
}