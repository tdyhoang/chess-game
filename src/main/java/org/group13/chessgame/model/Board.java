package org.group13.chessgame.model;

public class Board {
    private final Square[][] squares;
    public static final int SIZE = 8;

    public Board() {
        squares = new Square[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                squares[row][col] = new Square(row, col);
            }
        }
    }

    public Square getSquare(int row, int col) {
        if (isValidCoordinate(row, col)) {
            return squares[row][col];
        }
        return null;
    }

    public Piece getPiece(int row, int col) {
        Square square = getSquare(row, col);
        return (square != null) ? square.getPiece() : null;
    }

    public void setPiece(int row, int col, Piece piece) {
        Square square = getSquare(row, col);
        if (square != null) {
            square.setPiece(piece);
        }
    }

    public static boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public void initializeBoard() {
        // remove board
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                squares[r][c].setPiece(null);
            }
        }

        setPiece(0, 0, new Rook(PieceColor.BLACK)); // Rank 8
        setPiece(0, 1, new Knight(PieceColor.BLACK));
        setPiece(0, 2, new Bishop(PieceColor.BLACK));
        setPiece(0, 3, new Queen(PieceColor.BLACK));
        setPiece(0, 4, new King(PieceColor.BLACK));
        setPiece(0, 5, new Bishop(PieceColor.BLACK));
        setPiece(0, 6, new Knight(PieceColor.BLACK));
        setPiece(0, 7, new Rook(PieceColor.BLACK));
        for (int col = 0; col < SIZE; col++) {
            setPiece(1, col, new Pawn(PieceColor.BLACK)); // Rank 7
        }

        setPiece(7, 0, new Rook(PieceColor.WHITE)); // Rank 1
        setPiece(7, 1, new Knight(PieceColor.WHITE));
        setPiece(7, 2, new Bishop(PieceColor.WHITE));
        setPiece(7, 3, new Queen(PieceColor.WHITE));
        setPiece(7, 4, new King(PieceColor.WHITE));
        setPiece(7, 5, new Bishop(PieceColor.WHITE));
        setPiece(7, 6, new Knight(PieceColor.WHITE));
        setPiece(7, 7, new Rook(PieceColor.WHITE));
        for (int col = 0; col < SIZE; col++) {
            setPiece(6, col, new Pawn(PieceColor.WHITE)); // Rank 2
        }
    }

    public void applyMove(Move move) {
        Square start = move.getStartSquare();
        Square end = move.getEndSquare();
        Piece movedPiece = start.getPiece();

        if (movedPiece == null) {
            System.err.println("Lỗi: Không có quân cờ tại ô xuất phát của nước đi " + move);
            return;
        }

        movedPiece.setHasMoved(true);

        start.setPiece(null);

        Piece capturedPiece = move.getPieceCaptured();

        if (move.isPromotion()) {
            Piece promotedPiece = null;
            switch (move.getPromotionPieceType()) {
                case QUEEN: promotedPiece = new Queen(movedPiece.getColor()); break;
                case ROOK: promotedPiece = new Rook(movedPiece.getColor()); break;
                case BISHOP: promotedPiece = new Bishop(movedPiece.getColor()); break;
                case KNIGHT: promotedPiece = new Knight(movedPiece.getColor()); break;
                default:
                    System.err.println("Loại quân phong cấp không hợp lệ!");
                    promotedPiece = new Queen(movedPiece.getColor());
            }
            promotedPiece.setHasMoved(true);
            end.setPiece(promotedPiece);
        } else {
            end.setPiece(movedPiece);
        }


        if (move.isCastlingMove()) {
            Square rookStart = move.getRookStartSquareForCastling();
            Square rookEnd = move.getRookEndSquareForCastling();
            Piece rook = rookStart.getPiece();
            if (rook != null && rook.getType() == PieceType.ROOK) {
                rookStart.setPiece(null);
                rookEnd.setPiece(rook);
                rook.setHasMoved(true);
            } else {
                System.err.println("Lỗi nhập thành: Không tìm thấy Xe hoặc Xe không đúng vị trí.");
            }
        }

        if (move.isEnPassantMove()) {
            Square capturedPawnSquare = move.getEnPassantCaptureSquare();
            if (capturedPawnSquare != null && capturedPawnSquare.hasPiece()) {
                capturedPawnSquare.setPiece(null);
            } else {
                System.err.println("Lỗi bắt tốt qua đường: Không tìm thấy Tốt để bắt.");
            }
        }
    }

    public void undoMove(Move move) {
        Square start = move.getStartSquare();
        Square end = move.getEndSquare();
        Piece movedPiece = end.getPiece();

        if (move.isPromotion()) {
            start.setPiece(new Pawn(move.getPieceMoved().getColor()));
            if (move.getPieceMoved() instanceof Pawn) {
                start.getPiece().setHasMoved(move.getPieceMoved().hasMoved());
            }
        } else {
            start.setPiece(movedPiece);
        }

        end.setPiece(move.getPieceCaptured());

        if (move.isCastlingMove()) {
            Square rookStartOriginal = move.getRookStartSquareForCastling();
            Square rookEndOriginal = move.getRookEndSquareForCastling();
            Piece rook = rookEndOriginal.getPiece(); // Xe đang ở vị trí mới
            if (rook != null && rook.getType() == PieceType.ROOK) {
                rookEndOriginal.setPiece(null);
                rookStartOriginal.setPiece(rook);
                // rook.setHasMoved(false);
            }
        }

        if (move.isEnPassantMove()) {
            Square enPassantCapturedPawnSquare = move.getEnPassantCaptureSquare();
            Piece capturedPawn = move.getPieceCaptured();
            if (enPassantCapturedPawnSquare != null && capturedPawn != null) {
                enPassantCapturedPawnSquare.setPiece(capturedPawn);
                end.setPiece(null);
            }
        }
    }

    public Board copy() {
        Board newBoard = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Piece p = this.getPiece(r, c);
                if (p != null) {
                    Piece newPiece = null;
                    if (p instanceof Pawn) newPiece = new Pawn(p.getColor());
                    else if (p instanceof Rook) newPiece = new Rook(p.getColor());
                    else if (p instanceof Knight) newPiece = new Knight(p.getColor());
                    else if (p instanceof Bishop) newPiece = new Bishop(p.getColor());
                    else if (p instanceof Queen) newPiece = new Queen(p.getColor());
                    else if (p instanceof King) newPiece = new King(p.getColor());

                    if (newPiece != null) {
                        newPiece.setHasMoved(p.hasMoved());
                        newBoard.setPiece(r, c, newPiece);
                    }
                }
            }
        }
        return newBoard;
    }
}
