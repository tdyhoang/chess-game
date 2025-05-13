package org.group13.chessgame.model;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece {
    public Rook(PieceColor color) {
        super(color, PieceType.ROOK);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Game game, int currentRow, int currentCol) {
        List<Move> moves = new ArrayList<>();
        Board board = game.getBoard();
        Square startSquare = board.getSquare(currentRow, currentCol);

        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}}; // Right, Left, Down, Up

        for (int[] dir : directions) {
            for (int i = 1; i < Board.SIZE; i++) {
                int nextRow = currentRow + dir[0] * i;
                int nextCol = currentCol + dir[1] * i;

                if (!Board.isValidCoordinate(nextRow, nextCol)) {
                    break;
                }

                Square targetSquare = board.getSquare(nextRow, nextCol);
                if (targetSquare.isEmpty()) {
                    moves.add(new Move(startSquare, targetSquare, this));
                } else {
                    if (targetSquare.getPiece().getColor() != this.color) {
                        moves.add(new Move(startSquare, targetSquare, this));
                    }
                    break;
                }
            }
        }
        return moves;
    }
}
