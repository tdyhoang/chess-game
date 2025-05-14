package org.group13.chessgame;

import org.group13.chessgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class QueenTest {
    private Game game;
    private Board board;
    private Queen whiteQueen;

    @BeforeEach
    void setUp() {
        game = new Game();
        board = game.getBoard();
        whiteQueen = new Queen(PieceColor.WHITE);
    }

    private boolean containsMove(List<Move> moves, int startRow, int startCol, int endRow, int endCol) {
        return moves.stream().anyMatch(m ->
                m.getStartSquare().getRow() == startRow && m.getStartSquare().getCol() == startCol &&
                        m.getEndSquare().getRow() == endRow && m.getEndSquare().getCol() == endCol
        );
    }

    @Test
    @DisplayName("Queen on empty board from d4")
    void queenOnEmptyBoard() {
        board.setPiece(4, 3, whiteQueen);
        List<Move> moves = whiteQueen.getPseudoLegalMoves(game, 4, 3);
        assertEquals(27, moves.size());
    }

    @Test
    @DisplayName("Queen blocked and captures")
    void queenBlockedAndCaptures() {
        board.setPiece(4, 3, whiteQueen); // d4
        board.setPiece(4, 5, new Pawn(PieceColor.WHITE)); // f4
        board.setPiece(2, 3, new Pawn(PieceColor.BLACK)); // d6
        board.setPiece(2, 1, new Pawn(PieceColor.WHITE));  // b6
        board.setPiece(6, 5, new Pawn(PieceColor.BLACK)); // f2

        List<Move> moves = whiteQueen.getPseudoLegalMoves(game, 4, 3);
        assertEquals(19, moves.size());
    }
}
