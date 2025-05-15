package org.group13.chessgame;

import org.group13.chessgame.model.Pawn;
import org.group13.chessgame.model.Piece;
import org.group13.chessgame.model.PieceColor;
import org.group13.chessgame.model.Square;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SquareTest {
    @Test
    void testSquareCreationAndProperties() {
        Square square = new Square(3, 4); // d5
        assertEquals(3, square.getRow());
        assertEquals(4, square.getCol());
        assertTrue(square.isEmpty());
        assertNull(square.getPiece());
        assertEquals("e5", square.toString());
    }

    @Test
    void testSetAndGetPiece() {
        Square square = new Square(0, 0);
        Piece whitePawn = new Pawn(PieceColor.WHITE);
        square.setPiece(whitePawn);

        assertFalse(square.isEmpty());
        assertEquals(whitePawn, square.getPiece());
        assertTrue(square.hasPiece());
    }

    @Test
    void testHasEnemyOrAllyPiece() {
        Square square = new Square(0, 0);
        Piece whitePawn = new Pawn(PieceColor.WHITE);
        square.setPiece(whitePawn);

        assertTrue(square.hasAllyPiece(PieceColor.WHITE));
        assertFalse(square.hasEnemyPiece(PieceColor.WHITE));
        assertTrue(square.hasEnemyPiece(PieceColor.BLACK));
        assertFalse(square.hasAllyPiece(PieceColor.BLACK));
    }
}
