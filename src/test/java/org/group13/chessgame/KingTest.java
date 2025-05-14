package org.group13.chessgame;

import org.group13.chessgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class KingTest {
    private Game game;
    private Board board;
    private King whiteKing;

    @BeforeEach
    void setUp() {
        game = new Game();
        board = game.getBoard();
        whiteKing = new King(PieceColor.WHITE);
    }

    private boolean containsMove(List<Move> moves, int startRow, int startCol, int endRow, int endCol) {
        return moves.stream().anyMatch(m ->
                m.getStartSquare().getRow() == startRow && m.getStartSquare().getCol() == startCol &&
                        m.getEndSquare().getRow() == endRow && m.getEndSquare().getCol() == endCol
        );
    }

    @Test
    @DisplayName("King in center (d4), empty board")
    void kingInCenter() {
        board.setPiece(4, 3, whiteKing);
        List<Move> moves = whiteKing.getPseudoLegalMoves(game, 4, 3);
        assertEquals(8, moves.size());
        assertTrue(containsMove(moves, 4, 3, 3, 2)); // c5
        assertTrue(containsMove(moves, 4, 3, 5, 4)); // e3
    }

    @Test
    @DisplayName("King at corner (a1)")
    void kingAtCornerA1() {
        board.setPiece(7, 0, whiteKing);
        List<Move> moves = whiteKing.getPseudoLegalMoves(game, 7, 0);
        assertEquals(3, moves.size());
        assertTrue(containsMove(moves, 7, 0, 7, 1));
        assertTrue(containsMove(moves, 7, 0, 6, 0));
        assertTrue(containsMove(moves, 7, 0, 6, 1));
    }

    @Test
    @DisplayName("King blocked by ally pieces")
    void kingBlockedByAlly() {
        board.setPiece(4, 3, whiteKing); // d4
        board.setPiece(3, 3, new Pawn(PieceColor.WHITE)); // d5
        board.setPiece(4, 2, new Pawn(PieceColor.WHITE)); // c4

        List<Move> moves = whiteKing.getPseudoLegalMoves(game, 4, 3);
        assertEquals(6, moves.size());
        assertFalse(containsMove(moves, 4, 3, 3, 3)); // d5
        assertFalse(containsMove(moves, 4, 3, 4, 2)); // c4
        assertTrue(containsMove(moves, 4, 3, 3, 2)); // c5
    }

    @Test
    @DisplayName("King captures enemy pieces")
    void kingCapturesEnemy() {
        board.setPiece(4, 3, whiteKing); // d4
        board.setPiece(3, 3, new Pawn(PieceColor.BLACK)); // d5
        board.setPiece(4, 2, new Pawn(PieceColor.BLACK)); // c4

        List<Move> moves = whiteKing.getPseudoLegalMoves(game, 4, 3);
        assertEquals(8, moves.size());
        assertTrue(containsMove(moves, 4, 3, 3, 3)); // d5
        assertTrue(containsMove(moves, 4, 3, 4, 2)); // c4
    }
}
