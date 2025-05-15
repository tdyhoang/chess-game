package org.group13.chessgame;

import org.group13.chessgame.model.PieceColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PieceColorTest {
    @Test
    void testOppositeColor() {
        assertEquals(PieceColor.BLACK, PieceColor.WHITE.opposite(), "Opposite of WHITE should be BLACK");
        assertEquals(PieceColor.WHITE, PieceColor.BLACK.opposite(), "Opposite of BLACK should be WHITE");
    }
}
