package org.group13.chessgame.model;

import java.security.SecureRandom;
import java.util.Random;

public class ZobristTable {
    private static final int NUM_PIECE_TYPES = PieceType.values().length;   // 6
    private static final int NUM_COLORS = PieceColor.values().length;       // 2
    private static final int BOARD_SIZE = Board.SIZE;                       // 8
    private static final Random random = new SecureRandom();
    // pieceKeys[pieceType][color][row][col]
    private final long[][][][] pieceKeys;
    private final long blackToMoveKey;
    // castlingKeys[whiteOO][whiteOOO][blackOO][blackOOO]
    private final long[] castlingRightsKeys; // Index 0: WK, 1: WQ, 2: BK, 3: BQ
    // enPassantFileKeys[0_to_7]
    private final long[] enPassantFileKeys;

    public ZobristTable() {
        pieceKeys = new long[NUM_PIECE_TYPES][NUM_COLORS][BOARD_SIZE][BOARD_SIZE];
        for (int pt = 0; pt < NUM_PIECE_TYPES; pt++) {
            for (int c = 0; c < NUM_COLORS; c++) {
                for (int r = 0; r < BOARD_SIZE; r++) {
                    for (int cl = 0; cl < BOARD_SIZE; cl++) {
                        pieceKeys[pt][c][r][cl] = random.nextLong();
                    }
                }
            }
        }

        blackToMoveKey = random.nextLong();

        castlingRightsKeys = new long[4];
        for (int i = 0; i < 4; i++) {
            castlingRightsKeys[i] = random.nextLong();
        }

        enPassantFileKeys = new long[BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            enPassantFileKeys[i] = random.nextLong();
        }
    }

    public long getPieceKey(PieceType type, PieceColor color, int row, int col) {
        return pieceKeys[type.ordinal()][color.ordinal()][row][col];
    }

    public long getBlackToMoveKey() {
        return blackToMoveKey;
    }

    public long getCastlingRightsKey(int castlingIndex) { // 0=WK, 1=WQ, 2=BK, 3=BQ
        return castlingRightsKeys[castlingIndex];
    }

    public long getEnPassantFileKey(int file) { // 0-7
        return enPassantFileKeys[file];
    }
}