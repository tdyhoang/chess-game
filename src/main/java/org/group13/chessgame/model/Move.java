package org.group13.chessgame.model;

import org.group13.chessgame.utils.NotationUtils;

public class Move {
    private final Square startSquare;
    private final Square endSquare;
    private final Piece pieceMoved;
    private final boolean pieceMovedOriginalHasMoved;

    private Piece pieceCaptured;
    private PieceType promotionPieceType = null;

    private boolean isCastlingMove = false;
    private Square rookStartSquareForCastling = null;
    private Square rookEndSquareForCastling = null;
    private Piece pieceOnRookStartForCastling;
    private boolean rookOriginalHasMoved;

    private boolean isEnPassantMove = false;
    private Square enPassantCaptureSquare = null;

    private int halfMoveClockBeforeMove;
    private Square enPassantTargetSquareBeforeMove;
    private long hashGeneratedByThisMove;
    private String standardAlgebraicNotation;

    public Move(Square startSquare, Square endSquare, Piece pieceMoved, boolean pieceMovedOriginalHasMoved) {
        this.startSquare = startSquare;
        this.endSquare = endSquare;
        this.pieceMoved = pieceMoved;
        this.pieceMovedOriginalHasMoved = pieceMovedOriginalHasMoved;
        this.pieceCaptured = endSquare.hasPiece() && !this.isEnPassantMove() ? endSquare.getPiece() : null;
    }

    public Move(Square startSquare, Square endSquare, Piece pieceMoved, boolean pieceMovedOriginalHasMoved, PieceType promotionPieceType) {
        this(startSquare, endSquare, pieceMoved, pieceMovedOriginalHasMoved);
        this.promotionPieceType = promotionPieceType;
    }

    public Square getStartSquare() {
        return startSquare;
    }

    public Square getEndSquare() {
        return endSquare;
    }

    public Piece getPieceMoved() {
        return pieceMoved;
    }

    public Piece getPieceCaptured() {
        return pieceCaptured;
    }

    public void setPieceCaptured(Piece pieceCaptured) {
        this.pieceCaptured = pieceCaptured;
    }

    public boolean getPieceMovedOriginalHasMoved() {
        return pieceMovedOriginalHasMoved;
    }

    public void setRookInfoForCastlingUndo(Piece rook, boolean rookOriginalHasMoved) {
        this.pieceOnRookStartForCastling = rook;
        this.rookOriginalHasMoved = rookOriginalHasMoved;
    }

    public int getHalfMoveClockBeforeMove() {
        return halfMoveClockBeforeMove;
    }

    public void setHalfMoveClockBeforeMove(int clock) {
        this.halfMoveClockBeforeMove = clock;
    }

    public Square getEnPassantTargetSquareBeforeMove() {
        return enPassantTargetSquareBeforeMove;
    }

    public void setEnPassantTargetSquareBeforeMove(Square square) {
        this.enPassantTargetSquareBeforeMove = square;
    }

    public long getHashGenerated() {
        return hashGeneratedByThisMove;
    }

    public void setHashGenerated(long hash) {
        this.hashGeneratedByThisMove = hash;
    }

    public Piece getPieceOnRookStartForCastling() {
        return pieceOnRookStartForCastling;
    }

    public boolean getRookOriginalHasMoved() {
        return rookOriginalHasMoved;
    }

    public boolean isPromotion() {
        return promotionPieceType != null;
    }

    public PieceType getPromotionPieceType() {
        return promotionPieceType;
    }

    public boolean isCastlingMove() {
        return isCastlingMove;
    }

    public void setCastlingMove(boolean castlingMove) {
        isCastlingMove = castlingMove;
    }

    public Square getRookStartSquareForCastling() {
        return rookStartSquareForCastling;
    }

    public void setRookStartSquareForCastling(Square rookStartSquare) {
        this.rookStartSquareForCastling = rookStartSquare;
    }

    public Square getRookEndSquareForCastling() {
        return rookEndSquareForCastling;
    }

    public void setRookEndSquareForCastling(Square rookEndSquare) {
        this.rookEndSquareForCastling = rookEndSquare;
    }

    public boolean isEnPassantMove() {
        return isEnPassantMove;
    }

    public void setEnPassantMove(boolean enPassantMove) {
        isEnPassantMove = enPassantMove;
    }

    public Square getEnPassantCaptureSquare() {
        return enPassantCaptureSquare;
    }

    public void setEnPassantCaptureSquare(Square enPassantCaptureSquare) {
        this.enPassantCaptureSquare = enPassantCaptureSquare;
    }

    public String getStandardAlgebraicNotation() {
        return standardAlgebraicNotation;
    }

    public void setStandardAlgebraicNotation(String san) {
        this.standardAlgebraicNotation = san;
    }

    @Override
    public String toString() { // UCI string
        String start = NotationUtils.squareToAlgebraic(startSquare).toLowerCase();
        String end = NotationUtils.squareToAlgebraic(endSquare).toLowerCase();
        String promotion = "";
        if (isPromotion()) {
            promotion = Piece.pieceTypeToChar(promotionPieceType).toLowerCase();
        }
        return start + end + promotion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return startSquare.getRow() == move.startSquare.getRow() && startSquare.getCol() == move.startSquare.getCol() && endSquare.getRow() == move.endSquare.getRow() && endSquare.getCol() == move.endSquare.getCol() && promotionPieceType == move.promotionPieceType && isCastlingMove == move.isCastlingMove && isEnPassantMove == move.isEnPassantMove;
    }

    @Override
    public int hashCode() {
        int result = startSquare.getRow();
        result = 31 * result + startSquare.getCol();
        result = 31 * result + endSquare.getRow();
        result = 31 * result + endSquare.getCol();
        result = 31 * result + (promotionPieceType != null ? promotionPieceType.hashCode() : 0);
        result = 31 * result + (isCastlingMove ? 1 : 0);
        result = 31 * result + (isEnPassantMove ? 1 : 0);
        return result;
    }

    /**
     * Checks if this move is equivalent to another move based on player's intent
     * (start, end, and promotion). Ignores special flags like castling or en passant,
     * which are determined by game logic, not raw user input.
     *
     * @param other The other Move object to compare against.
     * @return true if the moves are equivalent from a UI perspective.
     */
    public boolean isEquivalent(Move other) {
        if (other == null) return false;

        return startSquare.getRow() == other.startSquare.getRow() && startSquare.getCol() == other.startSquare.getCol() && endSquare.getRow() == other.endSquare.getRow() && endSquare.getCol() == other.endSquare.getCol() && promotionPieceType == other.promotionPieceType;
    }
}
