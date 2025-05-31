package org.group13.chessgame.model;

public class Board {
    public static final int SIZE = 8;
    private final Square[][] squares;

    public Board() {
        squares = new Square[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                squares[row][col] = new Square(row, col);
            }
        }
    }

    public static boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    private static Piece getNewPiece(Piece p) {
        Piece newPiece = null;
        switch (p) {
            case Pawn ignored -> newPiece = new Pawn(p.getColor());
            case Rook ignored -> newPiece = new Rook(p.getColor());
            case Knight ignored -> newPiece = new Knight(p.getColor());
            case Bishop ignored -> newPiece = new Bishop(p.getColor());
            case Queen ignored -> newPiece = new Queen(p.getColor());
            case King ignored -> newPiece = new King(p.getColor());
            default -> {
            }
        }
        return newPiece;
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

        if (move.isPromotion()) {
            Piece promotedPiece = switch (move.getPromotionPieceType()) {
                case QUEEN -> new Queen(movedPiece.getColor());
                case ROOK -> new Rook(movedPiece.getColor());
                case BISHOP -> new Bishop(movedPiece.getColor());
                case KNIGHT -> new Knight(movedPiece.getColor());
                default -> {
                    System.err.println("Loại quân phong cấp không hợp lệ!");
                    yield new Queen(movedPiece.getColor());
                }
            };
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
        Piece originalPieceThatMoved = move.getPieceMoved();

        if (move.isPromotion()) {
            Pawn originalPawn = new Pawn(originalPieceThatMoved.getColor());
            originalPawn.setHasMoved(move.getPieceMovedOriginalHasMoved());
            start.setPiece(originalPawn);
        } else {
            originalPieceThatMoved.setHasMoved(move.getPieceMovedOriginalHasMoved());
            start.setPiece(originalPieceThatMoved);
        }

        if (move.isCastlingMove()) {
            Square rookStartOriginal = move.getRookStartSquareForCastling();
            Square rookEndOriginal = move.getRookEndSquareForCastling();
            Piece rook = move.getPieceOnRookStartForCastling();
            if (rook != null && rook.getType() == PieceType.ROOK) {
                rookEndOriginal.setPiece(null);
                rookStartOriginal.setPiece(rook);
                rook.setHasMoved(move.getRookOriginalHasMoved());
            } else {
                System.err.println("Lỗi undo castling: Không có thông tin Xe gốc.");
            }
        }

        if (move.isEnPassantMove()) {
            end.setPiece(null);
            Square capturedPawnSquare = move.getEnPassantCaptureSquare();
            if (capturedPawnSquare != null && move.getPieceCaptured() != null) {
                capturedPawnSquare.setPiece(move.getPieceCaptured());
            }
        } else {
            end.setPiece(move.getPieceCaptured());
        }
    }

    public Board copy() {
        Board newBoard = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Piece p = this.getPiece(r, c);
                if (p != null) {
                    Piece newPiece = getNewPiece(p);

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
