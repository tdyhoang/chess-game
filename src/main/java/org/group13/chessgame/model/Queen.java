package org.group13.chessgame.model;

import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece {
    public Queen(PieceColor color) {
        super(color, PieceType.QUEEN);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Game game, int currentRow, int currentCol) {
        List<Move> moves = new ArrayList<>();
        Board board = game.getBoard();
        Square startSquare = board.getSquare(currentRow, currentCol);

        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

        for (int[] dir : directions) {
            for (int i = 1; i < Board.SIZE; i++) {
                int nextRow = currentRow + dir[0] * i;
                int nextCol = currentCol + dir[1] * i;

                if (!Board.isValidCoordinate(nextRow, nextCol)) {
                    break;
                }

                Square targetSquare = board.getSquare(nextRow, nextCol);
                if (targetSquare.isEmpty()) {
                    moves.add(new Move(startSquare, targetSquare, this, this.hasMoved));
                } else {
                    if (targetSquare.getPiece().getColor() != this.color) {
                        moves.add(new Move(startSquare, targetSquare, this, this.hasMoved));
                    }
                    break;
                }
            }
        }
        return moves;
    }
}
