package org.group13.chessgame;

import org.group13.chessgame.model.Board;
import org.group13.chessgame.model.Piece;
import org.group13.chessgame.model.PieceColor;
import org.group13.chessgame.model.PieceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    @Test
    void testInitialBoardSetup() {
        board.initializeBoard();
        assertNotNull(board.getPiece(0, 0), "Black Rook should be at a8 (0,0)");
        assertEquals(PieceType.ROOK, board.getPiece(0, 0).getType());
        assertEquals(PieceColor.BLACK, board.getPiece(0, 0).getColor());

        assertNotNull(board.getPiece(1, 3), "Black Pawn should be at d7 (1,3)");
        assertEquals(PieceType.PAWN, board.getPiece(1, 3).getType());
        assertEquals(PieceColor.BLACK, board.getPiece(1, 3).getColor());

        assertNotNull(board.getPiece(7, 4), "White King should be at e1 (7,4)");
        assertEquals(PieceType.KING, board.getPiece(7, 4).getType());
        assertEquals(PieceColor.WHITE, board.getPiece(7, 4).getColor());

        assertNotNull(board.getPiece(6, 7), "White Pawn should be at h2 (6,7)");
        assertEquals(PieceType.PAWN, board.getPiece(6, 7).getType());
        assertEquals(PieceColor.WHITE, board.getPiece(6, 7).getColor());

        assertNull(board.getPiece(3, 3), "Square d5 (3,3) should be empty initially");
    }

    @Test
    void testGetSquareValidAndInvalid() {
        assertNotNull(board.getSquare(0, 0));
        assertNotNull(board.getSquare(7, 7));
        assertNull(board.getSquare(-1, 0), "Accessing row -1 should return null or throw");
        assertNull(board.getSquare(0, 8), "Accessing col 8 should return null or throw");
    }

    @Test
    void testBoardCopy() {
        board.initializeBoard();
        Piece originalPiece = board.getPiece(0,0); // Black Rook at a8

        Board copiedBoard = board.copy();

        assertNotNull(copiedBoard.getPiece(0,0));
        assertEquals(originalPiece.getType(), copiedBoard.getPiece(0,0).getType());
        assertEquals(originalPiece.getColor(), copiedBoard.getPiece(0,0).getColor());
        assertEquals(originalPiece.hasMoved(), copiedBoard.getPiece(0,0).hasMoved());


        assertNotSame(originalPiece, copiedBoard.getPiece(0,0), "Pieces on copied board should be new instances");

        board.setPiece(0,0, null); // Remove rook from original board
        assertNull(board.getPiece(0,0));
        assertNotNull(copiedBoard.getPiece(0,0), "Copied board should remain unchanged after original board is modified");

        copiedBoard.setPiece(1,0, null); // Remove black pawn from copied board
        assertNull(copiedBoard.getPiece(1,0));
        assertNotNull(board.getPiece(1,0), "Original board should remain unchanged after copied board is modified");
    }
}
