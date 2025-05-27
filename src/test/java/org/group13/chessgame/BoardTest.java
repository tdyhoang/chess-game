package org.group13.chessgame;

import org.group13.chessgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    private Board board;

    @BeforeEach
    void setUp() {
        Game game = new Game();
        board = game.getBoard();
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
        Piece originalPiece = board.getPiece(0, 0);

        Board copiedBoard = board.copy();

        assertNotNull(copiedBoard.getPiece(0, 0));
        assertEquals(originalPiece.getType(), copiedBoard.getPiece(0, 0).getType());
        assertEquals(originalPiece.getColor(), copiedBoard.getPiece(0, 0).getColor());
        assertEquals(originalPiece.hasMoved(), copiedBoard.getPiece(0, 0).hasMoved());


        assertNotSame(originalPiece, copiedBoard.getPiece(0, 0), "Pieces on copied board should be new instances");

        board.setPiece(0, 0, null);
        assertNull(board.getPiece(0, 0));
        assertNotNull(copiedBoard.getPiece(0, 0), "Copied board should remain unchanged after original board is modified");

        copiedBoard.setPiece(1, 0, null);
        assertNull(copiedBoard.getPiece(1, 0));
        assertNotNull(board.getPiece(1, 0), "Original board should remain unchanged after copied board is modified");
    }

    @Test
    @DisplayName("Apply and Undo simple pawn move (one step)")
    void testApplyUndoPawnOneStep() {
        Pawn whitePawn = new Pawn(PieceColor.WHITE);
        Square d2 = board.getSquare(6, 3);
        Square d3 = board.getSquare(5, 3);
        board.setPiece(d2.getRow(), d2.getCol(), whitePawn);
        assertFalse(whitePawn.hasMoved(), "Pawn should not have moved initially.");

        Move pawnMoveD2D3 = new Move(d2, d3, whitePawn, whitePawn.hasMoved());

        board.applyMove(pawnMoveD2D3);
        assertNull(board.getPiece(d2.getRow(), d2.getCol()), "d2 should be empty after move.");
        assertSame(whitePawn, board.getPiece(d3.getRow(), d3.getCol()), "Pawn should be at d3.");
        assertTrue(whitePawn.hasMoved(), "Pawn should have moved after applyMove.");
        assertEquals(whitePawn, pawnMoveD2D3.getPieceMoved());

        board.undoMove(pawnMoveD2D3);
        assertNull(board.getPiece(d3.getRow(), d3.getCol()), "d3 should be empty after undo.");
        assertSame(whitePawn, board.getPiece(d2.getRow(), d2.getCol()), "Pawn should be back at d2.");
        assertFalse(whitePawn.hasMoved(), "Pawn's hasMoved state should be restored after undo.");
    }

    @Test
    @DisplayName("Apply and Undo pawn initial two-step move")
    void testApplyUndoPawnTwoSteps() {
        Pawn whitePawn = new Pawn(PieceColor.WHITE);
        Square d2 = board.getSquare(6, 3);
        Square d4 = board.getSquare(4, 3);
        board.setPiece(d2.getRow(), d2.getCol(), whitePawn);
        assertFalse(whitePawn.hasMoved());

        Move pawnMoveD2D4 = new Move(d2, d4, whitePawn, whitePawn.hasMoved());

        board.applyMove(pawnMoveD2D4);
        assertNull(board.getPiece(d2.getRow(), d2.getCol()));
        assertSame(whitePawn, board.getPiece(d4.getRow(), d4.getCol()));
        assertTrue(whitePawn.hasMoved());

        board.undoMove(pawnMoveD2D4);
        assertNull(board.getPiece(d4.getRow(), d4.getCol()));
        assertSame(whitePawn, board.getPiece(d2.getRow(), d2.getCol()));
        assertFalse(whitePawn.hasMoved());
    }

    @Test
    @DisplayName("Apply and Undo rook move (no capture)")
    void testApplyUndoRookMove() {
        Rook whiteRook = new Rook(PieceColor.WHITE);
        Square a1 = board.getSquare(7, 0);
        Square a4 = board.getSquare(4, 0);
        board.setPiece(a1.getRow(), a1.getCol(), whiteRook);
        assertFalse(whiteRook.hasMoved());

        Move rookMoveA1A4 = new Move(a1, a4, whiteRook, whiteRook.hasMoved());

        board.applyMove(rookMoveA1A4);
        assertNull(board.getPiece(a1.getRow(), a1.getCol()));
        assertSame(whiteRook, board.getPiece(a4.getRow(), a4.getCol()));
        assertTrue(whiteRook.hasMoved());

        board.undoMove(rookMoveA1A4);
        assertNull(board.getPiece(a4.getRow(), a4.getCol()));
        assertSame(whiteRook, board.getPiece(a1.getRow(), a1.getCol()));
        assertFalse(whiteRook.hasMoved());
    }

    @Test
    @DisplayName("Apply and Undo knight move with capture")
    void testApplyUndoKnightCapture() {
        Knight whiteKnight = new Knight(PieceColor.WHITE);
        Pawn blackPawn = new Pawn(PieceColor.BLACK);

        Square g1 = board.getSquare(7, 6);
        Square f3 = board.getSquare(5, 5);
        board.setPiece(g1.getRow(), g1.getCol(), whiteKnight);
        board.setPiece(f3.getRow(), f3.getCol(), blackPawn);
        assertFalse(whiteKnight.hasMoved());

        Move knightMoveG1F3 = new Move(g1, f3, whiteKnight, whiteKnight.hasMoved());
        assertSame(blackPawn, knightMoveG1F3.getPieceCaptured(), "Move object should register the black pawn as captured.");

        board.applyMove(knightMoveG1F3);
        assertNull(board.getPiece(g1.getRow(), g1.getCol()));
        assertSame(whiteKnight, board.getPiece(f3.getRow(), f3.getCol()), "Knight should be at f3.");
        assertTrue(whiteKnight.hasMoved());

        board.undoMove(knightMoveG1F3);
        assertSame(whiteKnight, board.getPiece(g1.getRow(), g1.getCol()), "Knight should be back at g1.");
        assertSame(blackPawn, board.getPiece(f3.getRow(), f3.getCol()), "Captured black pawn should be back at f3 after undo.");
        assertFalse(whiteKnight.hasMoved());
    }

    @Test
    @DisplayName("Apply and Undo pawn promotion (no capture)")
    void testApplyUndoPawnPromotion() {
        Pawn whitePawn = new Pawn(PieceColor.WHITE);
        Square a7 = board.getSquare(1, 0);
        Square a8 = board.getSquare(0, 0);
        board.setPiece(a7.getRow(), a7.getCol(), whitePawn);
        whitePawn.setHasMoved(true);

        Move promotionMove = new Move(a7, a8, whitePawn, whitePawn.hasMoved(), PieceType.QUEEN);

        board.applyMove(promotionMove);
        assertNull(board.getPiece(a7.getRow(), a7.getCol()), "a7 should be empty after promotion.");
        Piece promotedPiece = board.getPiece(a8.getRow(), a8.getCol());
        assertNotNull(promotedPiece, "a8 should have a piece after promotion.");
        assertEquals(PieceType.QUEEN, promotedPiece.getType(), "Promoted piece should be a Queen.");
        assertEquals(PieceColor.WHITE, promotedPiece.getColor(), "Promoted piece should be White.");
        assertTrue(promotedPiece.hasMoved(), "Promoted Queen should be marked as moved.");

        board.undoMove(promotionMove);
        assertNull(board.getPiece(a8.getRow(), a8.getCol()), "a8 should be empty after undoing promotion.");
        Piece originalPawn = board.getPiece(a7.getRow(), a7.getCol());
        assertNotNull(originalPawn, "Pawn should be back at a7.");
        assertEquals(PieceType.PAWN, originalPawn.getType(), "Piece at a7 should be a Pawn after undo.");
        assertEquals(PieceColor.WHITE, originalPawn.getColor());
        assertTrue(originalPawn.hasMoved(), "Pawn's original hasMoved state (true) should be restored.");
    }

    @Test
    @DisplayName("Apply and Undo pawn promotion with capture")
    void testApplyUndoPawnPromotionWithCapture() {
        Pawn whitePawn = new Pawn(PieceColor.WHITE);
        Rook blackRook = new Rook(PieceColor.BLACK);

        Square b7 = board.getSquare(1, 1);
        Square c8 = board.getSquare(0, 2);
        board.setPiece(b7.getRow(), b7.getCol(), whitePawn);
        board.setPiece(c8.getRow(), c8.getCol(), blackRook);
        whitePawn.setHasMoved(true);

        Move promotionCaptureMove = new Move(b7, c8, whitePawn, whitePawn.hasMoved(), PieceType.QUEEN);
        assertSame(blackRook, promotionCaptureMove.getPieceCaptured());

        board.applyMove(promotionCaptureMove);
        assertNull(board.getPiece(b7.getRow(), b7.getCol()));
        Piece promotedQueen = board.getPiece(c8.getRow(), c8.getCol());
        assertNotNull(promotedQueen);
        assertEquals(PieceType.QUEEN, promotedQueen.getType());
        assertEquals(PieceColor.WHITE, promotedQueen.getColor());

        board.undoMove(promotionCaptureMove);
        assertSame(blackRook, board.getPiece(c8.getRow(), c8.getCol()), "Captured black rook should be back at c8.");
        Piece originalPawn = board.getPiece(b7.getRow(), b7.getCol());
        assertNotNull(originalPawn);
        assertEquals(PieceType.PAWN, originalPawn.getType());
        assertTrue(originalPawn.hasMoved());
    }
}
