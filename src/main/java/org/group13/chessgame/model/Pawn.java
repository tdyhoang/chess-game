package org.group13.chessgame.model;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {
    public Pawn(PieceColor color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Game game, int currentRow, int currentCol) {
        List<Move> moves = new ArrayList<>();
        Board board = game.getBoard();
        int direction = (this.color == PieceColor.WHITE) ? -1 : 1; // WHITE moves from high -> low row (7->0), BLACK move from low -> high row (0->7)

        int oneStepRow = currentRow + direction;
        if (Board.isValidCoordinate(oneStepRow, currentCol) && board.getSquare(oneStepRow, currentCol).isEmpty()) {
            addPawnMove(moves, board.getSquare(currentRow, currentCol), board.getSquare(oneStepRow, currentCol), this.color, this.hasMoved);

            if (!this.hasMoved()) {
                int twoStepsRow = currentRow + 2 * direction;
                if (Board.isValidCoordinate(twoStepsRow, currentCol) && board.getSquare(twoStepsRow, currentCol).isEmpty()) {
                    moves.add(new Move(board.getSquare(currentRow, currentCol), board.getSquare(twoStepsRow, currentCol), this, this.hasMoved));
                }
            }
        }

        int[] captureCols = {currentCol - 1, currentCol + 1};
        for (int captureCol : captureCols) {
            if (Board.isValidCoordinate(oneStepRow, captureCol)) {
                Square targetSquare = board.getSquare(oneStepRow, captureCol);
                if (targetSquare.hasPiece() && targetSquare.getPiece().getColor() != this.color) {
                    addPawnMove(moves, board.getSquare(currentRow, currentCol), targetSquare, this.color, this.hasMoved);
                }
            }
        }

        Move lastMove = game.getLastMove();
        if (lastMove != null &&
                lastMove.getPieceMoved().getType() == PieceType.PAWN &&
                Math.abs(lastMove.getStartSquare().getRow() - lastMove.getEndSquare().getRow()) == 2 &&
                lastMove.getEndSquare().getRow() == currentRow &&
                Math.abs(lastMove.getEndSquare().getCol() - currentCol) == 1) {

            Square targetSquareForMove = board.getSquare(currentRow + direction, lastMove.getEndSquare().getCol());
            Square capturedPawnSquare = lastMove.getEndSquare();

            if (targetSquareForMove != null && targetSquareForMove.isEmpty()) {
                Move enPassantMove = new Move(board.getSquare(currentRow, currentCol), targetSquareForMove, this, this.hasMoved);
                enPassantMove.setEnPassantMove(true);
                enPassantMove.setPieceCaptured(capturedPawnSquare.getPiece());
                enPassantMove.setEnPassantCaptureSquare(capturedPawnSquare);
                moves.add(enPassantMove);
            }
        }

        return moves;
    }

    private void addPawnMove(List<Move> moves, Square startSquare, Square endSquare, PieceColor pawnColor, boolean originalHasMoved) {
        int promotionRank = (pawnColor == PieceColor.WHITE) ? 0 : (Board.SIZE - 1);
        if (endSquare.getRow() == promotionRank) {
            moves.add(new Move(startSquare, endSquare, this, originalHasMoved, PieceType.QUEEN));
            moves.add(new Move(startSquare, endSquare, this, originalHasMoved, PieceType.ROOK));
            moves.add(new Move(startSquare, endSquare, this, originalHasMoved, PieceType.BISHOP));
            moves.add(new Move(startSquare, endSquare, this, originalHasMoved, PieceType.KNIGHT));
        } else {
            moves.add(new Move(startSquare, endSquare, this, originalHasMoved));
        }
    }
}
