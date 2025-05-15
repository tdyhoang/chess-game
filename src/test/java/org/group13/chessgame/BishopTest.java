package org.group13.chessgame;

import org.group13.chessgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BishopTest {
    private Game game;
    private Board board;
    private Bishop whiteBishop;

    @BeforeEach
    void setUp() {
        game = new Game();
        board = game.getBoard();
        whiteBishop = new Bishop(PieceColor.WHITE);
    }

    private boolean containsMove(List<Move> moves, int startRow, int startCol, int endRow, int endCol) {
        return moves.stream().anyMatch(m -> m.getStartSquare().getRow() == startRow && m.getStartSquare().getCol() == startCol && m.getEndSquare().getRow() == endRow && m.getEndSquare().getCol() == endCol);
    }

    @Test
    @DisplayName("Bishop on empty board from d4")
    void bishopOnEmptyBoard() {
        board.setPiece(4, 3, whiteBishop);
        List<Move> moves = whiteBishop.getPseudoLegalMoves(game, 4, 3);
        assertEquals(13, moves.size());
        assertTrue(containsMove(moves, 4, 3, 1, 0), "Should move to a7");
        assertTrue(containsMove(moves, 4, 3, 0, 7), "Should move to h8");
        assertTrue(containsMove(moves, 4, 3, 7, 0), "Should move to a1");
        assertTrue(containsMove(moves, 4, 3, 7, 6), "Should move to g1");
    }

    @Test
    @DisplayName("Bishop on c1 (light square)")
    void bishopOnC1() {
        board.setPiece(7, 2, whiteBishop);
        List<Move> moves = whiteBishop.getPseudoLegalMoves(game, 7, 2);
        assertEquals(7, moves.size());
    }

    @Test
    @DisplayName("Bishop blocked by ally and captures enemy")
    void bishopBlockedAndCaptures() {
        board.setPiece(4, 3, whiteBishop); // d4
        board.setPiece(2, 1, new Pawn(PieceColor.WHITE));  // b6
        board.setPiece(6, 5, new Pawn(PieceColor.BLACK)); // f2

        List<Move> moves = whiteBishop.getPseudoLegalMoves(game, 4, 3);
        assertEquals(10, moves.size());
        assertTrue(containsMove(moves, 4, 3, 3, 2), "Should move to c5 (NW)");
        assertFalse(containsMove(moves, 4, 3, 2, 1), "Should not move to b6 (ally block)");
        assertTrue(containsMove(moves, 4, 3, 6, 5), "Should capture at f2 (SE)");
        assertFalse(containsMove(moves, 4, 3, 7, 6), "Should not move past captured f2 (to g1)");
    }
}
