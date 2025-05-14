package org.group13.chessgame.model;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {
    public King(PieceColor color) {
        super(color, PieceType.KING);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Game game, int currentRow, int currentCol) {
        List<Move> moves = new ArrayList<>();
        Board board = game.getBoard();
        Square startSquare = board.getSquare(currentRow, currentCol);

        int[][] kingMoves = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1}, {1, 0}, {1, 1}
        };

        for (int[] moveOffset : kingMoves) {
            int nextRow = currentRow + moveOffset[0];
            int nextCol = currentCol + moveOffset[1];

            if (Board.isValidCoordinate(nextRow, nextCol)) {
                Square targetSquare = board.getSquare(nextRow, nextCol);
                if (targetSquare.isEmpty() || targetSquare.getPiece().getColor() != this.color) {
                    moves.add(new Move(startSquare, targetSquare, this, this.hasMoved));
                }
            }
        }

        // Castling is handled by `Game.getAllLegalMovesForPlayer()`, not here

        return moves;
    }
}
