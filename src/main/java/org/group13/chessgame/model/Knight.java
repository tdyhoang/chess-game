package org.group13.chessgame.model;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {
    public Knight(PieceColor color) {
        super(color, PieceType.KNIGHT);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Game game, int currentRow, int currentCol) {
        List<Move> moves = new ArrayList<>();
        Board board = game.getBoard();
        Square startSquare = board.getSquare(currentRow, currentCol);

        int[][] knightMoves = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        for (int[] moveOffset : knightMoves) {
            int nextRow = currentRow + moveOffset[0];
            int nextCol = currentCol + moveOffset[1];

            if (Board.isValidCoordinate(nextRow, nextCol)) {
                Square targetSquare = board.getSquare(nextRow, nextCol);
                if (targetSquare.isEmpty() || targetSquare.getPiece().getColor() != this.color) {
                    moves.add(new Move(startSquare, targetSquare, this, this.hasMoved));
                }
            }
        }
        return moves;
    }
}
