package org.group13.chessgame;

import org.group13.chessgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class KnightTest {
    private Game game;
    private Board board;
    private Knight whiteKnight;

    @BeforeEach
    void setUp() {
        game = new Game();
        board = game.getBoard();
        whiteKnight = new Knight(PieceColor.WHITE);
    }

    private boolean containsMove(List<Move> moves, int startRow, int startCol, int endRow, int endCol) {
        return moves.stream().anyMatch(m ->
                m.getStartSquare().getRow() == startRow && m.getStartSquare().getCol() == startCol &&
                        m.getEndSquare().getRow() == endRow && m.getEndSquare().getCol() == endCol
        );
    }

    @Test
    @DisplayName("Knight in center (d4), empty board")
    void knightInCenter() {
        board.setPiece(4, 3, whiteKnight);
        List<Move> moves = whiteKnight.getPseudoLegalMoves(game, 4, 3);
        assertEquals(8, moves.size(), "Knight in center should have 8 moves on empty board.");
        assertTrue(containsMove(moves, 4, 3, 2, 2)); // c6
        assertTrue(containsMove(moves, 4, 3, 6, 4)); // e2
    }

    @Test
    @DisplayName("Knight at corner (a1)")
    void knightAtCornerA1() {
        board.setPiece(7, 0, whiteKnight);
        List<Move> moves = whiteKnight.getPseudoLegalMoves(game, 7, 0);
        assertEquals(2, moves.size());
        assertTrue(containsMove(moves, 7, 0, 5, 1)); // b3
        assertTrue(containsMove(moves, 7, 0, 6, 2)); // c2
    }

    @Test
    @DisplayName("Knight blocked by ally pieces")
    void knightBlockedByAlly() {
        board.setPiece(4, 3, whiteKnight); // d4
        board.setPiece(2, 2, new Pawn(PieceColor.WHITE)); // c6
        board.setPiece(6, 4, new Pawn(PieceColor.WHITE)); // e2

        List<Move> moves = whiteKnight.getPseudoLegalMoves(game, 4, 3);
        assertEquals(6, moves.size(), "Knight should have 6 moves if 2 targets are blocked by allies.");
        assertFalse(containsMove(moves, 4, 3, 2, 2), "Should not move to c6 (ally block)");
        assertFalse(containsMove(moves, 4, 3, 6, 4), "Should not move to e2 (ally block)");
        assertTrue(containsMove(moves, 4, 3, 2, 4), "Should still move to e6 (unblocked)");
    }

    @Test
    @DisplayName("Knight captures enemy pieces")
    void knightCapturesEnemy() {
        board.setPiece(4, 3, whiteKnight); // d4
        board.setPiece(2, 2, new Pawn(PieceColor.BLACK)); // c6
        board.setPiece(6, 4, new Pawn(PieceColor.BLACK)); // e2

        List<Move> moves = whiteKnight.getPseudoLegalMoves(game, 4, 3);
        assertEquals(8, moves.size());
        assertTrue(containsMove(moves, 4, 3, 2, 2), "Should be able to capture at c6");
        assertTrue(containsMove(moves, 4, 3, 6, 4), "Should be able to capture at e2");
    }
}
