package org.group13.chessgame;

import org.group13.chessgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RookTest {
    private Game game;
    private Board board;
    private Rook whiteRook;

    @BeforeEach
    void setUp() {
        game = new Game();
        board = game.getBoard();
        whiteRook = new Rook(PieceColor.WHITE);
    }

    private boolean containsMove(List<Move> moves, int startRow, int startCol, int endRow, int endCol) {
        return moves.stream().anyMatch(m -> m.getStartSquare().getRow() == startRow && m.getStartSquare().getCol() == startCol && m.getEndSquare().getRow() == endRow && m.getEndSquare().getCol() == endCol);
    }

    @Test
    @DisplayName("Rook on empty board from d4")
    void rookOnEmptyBoard() {
        board.setPiece(4, 3, whiteRook);
        List<Move> moves = whiteRook.getPseudoLegalMoves(game, 4, 3);

        assertEquals(14, moves.size(), "Rook on empty board from d4 should have 14 moves.");
        assertTrue(containsMove(moves, 4, 3, 0, 3), "Should move to d8"); // (0,3)
        assertTrue(containsMove(moves, 4, 3, 7, 3), "Should move to d1"); // (7,3)
        assertTrue(containsMove(moves, 4, 3, 4, 0), "Should move to a4"); // (4,0)
        assertTrue(containsMove(moves, 4, 3, 4, 7), "Should move to h4"); // (4,7)
    }

    @Test
    @DisplayName("Rook blocked by ally pieces")
    void rookBlockedByAlly() {
        board.setPiece(4, 3, whiteRook); // d4
        board.setPiece(4, 5, new Pawn(PieceColor.WHITE)); // f4
        board.setPiece(2, 3, new Pawn(PieceColor.WHITE)); // d6

        List<Move> moves = whiteRook.getPseudoLegalMoves(game, 4, 3);
        assertEquals(8, moves.size());
        assertTrue(containsMove(moves, 4, 3, 4, 4), "Should move to e4 (right, before ally block)");
        assertFalse(containsMove(moves, 4, 3, 4, 5), "Should not move to f4 (ally block)");
        assertTrue(containsMove(moves, 4, 3, 3, 3), "Should move to d5 (up, before ally block)");
        assertFalse(containsMove(moves, 4, 3, 2, 3), "Should not move to d6 (ally block)");
    }

    @Test
    @DisplayName("Rook captures enemy pieces")
    void rookCapturesEnemy() {
        board.setPiece(4, 3, whiteRook); // d4
        board.setPiece(4, 5, new Pawn(PieceColor.BLACK)); // f4
        board.setPiece(2, 3, new Pawn(PieceColor.BLACK)); // d6

        List<Move> moves = whiteRook.getPseudoLegalMoves(game, 4, 3);
        assertEquals(10, moves.size());
        assertTrue(containsMove(moves, 4, 3, 4, 5), "Should be able to capture at f4");
        assertFalse(containsMove(moves, 4, 3, 4, 6), "Should not move past captured enemy at f4");
        assertTrue(containsMove(moves, 4, 3, 2, 3), "Should be able to capture at d6");
        assertFalse(containsMove(moves, 4, 3, 1, 3), "Should not move past captured enemy at d6");
    }

    @Test
    @DisplayName("Rook at corner a1")
    void rookAtCornerA1() {
        board.setPiece(7, 0, whiteRook); // a1
        List<Move> moves = whiteRook.getPseudoLegalMoves(game, 7, 0);
        assertEquals(14, moves.size());
        assertTrue(containsMove(moves, 7, 0, 7, 7)); // a1 -> h1
        assertTrue(containsMove(moves, 7, 0, 0, 0)); // a1 -> a8
    }
}
