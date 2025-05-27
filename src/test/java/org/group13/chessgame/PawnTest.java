package org.group13.chessgame;

import org.group13.chessgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PawnTest {
    private Game game;
    private Board board;

    @BeforeEach
    void setUp() {
        game = new Game();
        board = game.getBoard();
    }

    private boolean containsMove(List<Move> moves, int startRow, int startCol, int endRow, int endCol) {
        return moves.stream().anyMatch(m -> m.getStartSquare().getRow() == startRow && m.getStartSquare().getCol() == startCol && m.getEndSquare().getRow() == endRow && m.getEndSquare().getCol() == endCol && !m.isPromotion());
    }

    private boolean containsPromotionMove(List<Move> moves, int startRow, int startCol, int endRow, int endCol, PieceType promotionType) {
        return moves.stream().anyMatch(m -> m.getStartSquare().getRow() == startRow && m.getStartSquare().getCol() == startCol && m.getEndSquare().getRow() == endRow && m.getEndSquare().getCol() == endCol && m.isPromotion() && m.getPromotionPieceType() == promotionType);
    }

    @Nested
    @DisplayName("White Pawn Moves")
    class WhitePawnMoves {
        private Pawn whitePawn;

        @Test
        @DisplayName("Initial move: one and two steps forward")
        void initialMove() {
            whitePawn = new Pawn(PieceColor.WHITE);
            board.setPiece(6, 3, whitePawn); // d2

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 6, 3);

            assertEquals(2, moves.size(), "Should have 2 moves from initial position (d2).");
            assertTrue(containsMove(moves, 6, 3, 5, 3), "Should be able to move to d3.");
            assertTrue(containsMove(moves, 6, 3, 4, 3), "Should be able to move to d4.");
        }

        @Test
        @DisplayName("Initial move blocked one step forward")
        void initialMoveBlockedOneStep() {
            whitePawn = new Pawn(PieceColor.WHITE);
            board.setPiece(6, 3, whitePawn); // d2
            board.setPiece(5, 3, new Pawn(PieceColor.BLACK)); // d3

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 6, 3);
            assertEquals(0, moves.size(), "Should have 0 moves if one step forward is blocked.");
        }

        @Test
        @DisplayName("Initial move blocked two steps forward")
        void initialMoveBlockedTwoSteps() {
            whitePawn = new Pawn(PieceColor.WHITE);
            board.setPiece(6, 3, whitePawn); // d2
            board.setPiece(4, 3, new Pawn(PieceColor.BLACK)); // d4

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 6, 3);
            assertEquals(1, moves.size(), "Should have 1 move (one step) if two steps forward is blocked.");
            assertTrue(containsMove(moves, 6, 3, 5, 3), "Should be able to move to d3.");
        }

        @Test
        @DisplayName("Non-initial move: one step forward")
        void nonInitialMoveOneStep() {
            whitePawn = new Pawn(PieceColor.WHITE);
            whitePawn.setHasMoved(true);
            board.setPiece(5, 3, whitePawn); // d3

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 5, 3);
            assertEquals(1, moves.size(), "Should have 1 move from non-initial position.");
            assertTrue(containsMove(moves, 5, 3, 4, 3), "Should be able to move to d4.");
        }

        @Test
        @DisplayName("Capture diagonally left and right")
        void captureDiagonally() {
            whitePawn = new Pawn(PieceColor.WHITE);
            whitePawn.setHasMoved(true);
            board.setPiece(3, 3, whitePawn); // d5
            board.setPiece(2, 2, new Pawn(PieceColor.BLACK)); // c6
            board.setPiece(2, 4, new Pawn(PieceColor.BLACK)); // e6

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 3, 3);
            assertEquals(3, moves.size(), "Should have 3 moves (1 forward, 2 captures).");
            assertTrue(containsMove(moves, 3, 3, 2, 3), "Should be able to move to d6.");
            assertTrue(containsMove(moves, 3, 3, 2, 2), "Should be able to capture at c6.");
            assertTrue(containsMove(moves, 3, 3, 2, 4), "Should be able to capture at e6.");
        }

        @Test
        @DisplayName("Cannot capture ally piece")
        void cannotCaptureAlly() {
            whitePawn = new Pawn(PieceColor.WHITE);
            whitePawn.setHasMoved(true);
            board.setPiece(3, 3, whitePawn); // d5
            board.setPiece(2, 2, new Rook(PieceColor.WHITE)); // Rc6

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 3, 3);
            assertEquals(1, moves.stream().filter(m -> !m.isPromotion()).count());
            assertTrue(containsMove(moves, 3, 3, 2, 3));
            assertFalse(containsMove(moves, 3, 3, 2, 2));
        }

        @Test
        @DisplayName("Promotion to Queen, Rook, Bishop, Knight")
        void promotion() {
            whitePawn = new Pawn(PieceColor.WHITE);
            board.setPiece(1, 0, whitePawn); // a7

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 1, 0);

            long promotionMovesCount = moves.stream().filter(Move::isPromotion).count();
            assertEquals(4, promotionMovesCount, "Should have 4 promotion moves.");
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 0, PieceType.QUEEN));
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 0, PieceType.ROOK));
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 0, PieceType.BISHOP));
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 0, PieceType.KNIGHT));
        }

        @Test
        @DisplayName("Promotion with capture")
        void promotionWithCapture() {
            whitePawn = new Pawn(PieceColor.WHITE);
            board.setPiece(1, 0, whitePawn); // a7
            board.setPiece(0, 1, new Rook(PieceColor.BLACK)); // Rb8

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 1, 0);

            assertEquals(8, moves.size());

            assertTrue(containsPromotionMove(moves, 1, 0, 0, 1, PieceType.QUEEN), "Should promote to Queen by capturing on b8");
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 1, PieceType.ROOK), "Should promote to Rook by capturing on b8");
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 1, PieceType.BISHOP), "Should promote to Bishop by capturing on b8");
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 1, PieceType.KNIGHT), "Should promote to Knight by capturing on b8");
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 0, PieceType.QUEEN));
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 0, PieceType.ROOK));
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 0, PieceType.BISHOP));
            assertTrue(containsPromotionMove(moves, 1, 0, 0, 0, PieceType.KNIGHT));
        }
    }

    @Nested
    @DisplayName("Black Pawn Moves")
    class BlackPawnMoves {
        private Pawn blackPawn;

        @Test
        @DisplayName("Initial move: one and two steps forward")
        void initialMove() {
            blackPawn = new Pawn(PieceColor.BLACK);
            board.setPiece(1, 3, blackPawn); // d7

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 1, 3);
            assertEquals(2, moves.size(), "Should have 2 moves from initial position (d7).");
            assertTrue(containsMove(moves, 1, 3, 2, 3), "Should be able to move to d6.");
            assertTrue(containsMove(moves, 1, 3, 3, 3), "Should be able to move to d5.");
        }

        @Test
        @DisplayName("Initial move blocked one step forward")
        void initialMoveBlockedOneStep() {
            blackPawn = new Pawn(PieceColor.BLACK);
            board.setPiece(1, 3, blackPawn); // d7
            board.setPiece(2, 3, new Pawn(PieceColor.WHITE)); // d6

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 1, 3);
            assertEquals(0, moves.size(), "Should have 0 moves if one step forward is blocked.");
        }

        @Test
        @DisplayName("Initial move blocked two steps forward")
        void initialMoveBlockedTwoSteps() {
            blackPawn = new Pawn(PieceColor.BLACK);
            board.setPiece(1, 3, blackPawn); // d7
            board.setPiece(3, 3, new Pawn(PieceColor.WHITE)); // d5

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 1, 3);
            assertEquals(1, moves.size(), "Should have 1 move (one step) if two steps forward is blocked.");
            assertTrue(containsMove(moves, 1, 3, 2, 3), "Should be able to move to d6.");
        }

        @Test
        @DisplayName("Non-initial move: one step forward")
        void nonInitialMoveOneStep() {
            blackPawn = new Pawn(PieceColor.BLACK);
            blackPawn.setHasMoved(true);
            board.setPiece(2, 3, blackPawn); // d6

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 2, 3);
            assertEquals(1, moves.size(), "Should have 1 move from non-initial position.");
            assertTrue(containsMove(moves, 2, 3, 3, 3), "Should be able to move to d5.");
        }

        @Test
        @DisplayName("Capture diagonally left and right")
        void captureDiagonally() {
            blackPawn = new Pawn(PieceColor.BLACK);
            blackPawn.setHasMoved(true);
            board.setPiece(4, 3, blackPawn); // d5
            board.setPiece(5, 2, new Pawn(PieceColor.WHITE)); // c6
            board.setPiece(5, 4, new Pawn(PieceColor.WHITE)); // e6

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 4, 3);
            assertEquals(3, moves.size(), "Should have 3 moves (1 forward, 2 captures).");
            assertTrue(containsMove(moves, 4, 3, 5, 3), "Should be able to move to d6.");
            assertTrue(containsMove(moves, 4, 3, 5, 2), "Should be able to capture at c6.");
            assertTrue(containsMove(moves, 4, 3, 5, 4), "Should be able to capture at e6.");
        }

        @Test
        @DisplayName("Cannot capture ally piece")
        void cannotCaptureAlly() {
            blackPawn = new Pawn(PieceColor.BLACK);
            blackPawn.setHasMoved(true);
            board.setPiece(4, 3, blackPawn); // d5
            board.setPiece(5, 2, new Rook(PieceColor.BLACK)); // Rc6

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 4, 3);
            assertEquals(1, moves.stream().filter(m -> !m.isPromotion()).count());
            assertTrue(containsMove(moves, 4, 3, 5, 3));
            assertFalse(containsMove(moves, 4, 3, 5, 2));
        }

        @Test
        @DisplayName("Promotion to Queen, Rook, Bishop, Knight")
        void promotion() {
            blackPawn = new Pawn(PieceColor.BLACK);
            board.setPiece(6, 0, blackPawn); // a2

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 6, 0);

            long promotionMovesCount = moves.stream().filter(Move::isPromotion).count();
            assertEquals(4, promotionMovesCount, "Should have 4 promotion moves.");
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 0, PieceType.QUEEN));
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 0, PieceType.ROOK));
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 0, PieceType.BISHOP));
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 0, PieceType.KNIGHT));
        }

        @Test
        @DisplayName("Promotion with capture")
        void promotionWithCapture() {
            blackPawn = new Pawn(PieceColor.BLACK);
            board.setPiece(6, 0, blackPawn); // a2
            board.setPiece(7, 1, new Rook(PieceColor.WHITE)); // Rb1

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 6, 0);

            assertEquals(8, moves.size());

            assertTrue(containsPromotionMove(moves, 6, 0, 7, 1, PieceType.QUEEN), "Should promote to Queen by capturing on b1");
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 1, PieceType.ROOK), "Should promote to Rook by capturing on b1");
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 1, PieceType.BISHOP), "Should promote to Bishop by capturing on b1");
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 1, PieceType.KNIGHT), "Should promote to Knight by capturing on b1");
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 0, PieceType.QUEEN));
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 0, PieceType.ROOK));
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 0, PieceType.BISHOP));
            assertTrue(containsPromotionMove(moves, 6, 0, 7, 0, PieceType.KNIGHT));
        }
    }

    @Nested
    @DisplayName("En Passant")
    class EnPassantTests {
        @Test
        @DisplayName("White Pawn can perform en passant capture")
        void whitePawnEnPassant() {
            Pawn whitePawn = new Pawn(PieceColor.WHITE);
            board.setPiece(3, 3, whitePawn); // d5

            Pawn blackPawnOriginal = new Pawn(PieceColor.BLACK);

            Square blackPawnEndSquare = board.getSquare(3, 2);
            board.setPiece(blackPawnEndSquare.getRow(), blackPawnEndSquare.getCol(), blackPawnOriginal);

            Move blackLastMove = new Move(new Square(1, 2), blackPawnEndSquare, blackPawnOriginal, false); // c7-c5
            game.getMoveHistory().add(blackLastMove);

            List<Move> moves = whitePawn.getPseudoLegalMoves(game, 3, 3);

            boolean foundEnPassant = moves.stream().anyMatch(m -> m.isEnPassantMove() && m.getStartSquare().getRow() == 3 && m.getStartSquare().getCol() == 3 && // d5
                    m.getEndSquare().getRow() == 2 && m.getEndSquare().getCol() == 2 &&   // c6
                    m.getPieceCaptured() == blackPawnOriginal && m.getEnPassantCaptureSquare() == blackPawnEndSquare && m.getEnPassantCaptureSquare().getPiece() == blackPawnOriginal);
            assertTrue(foundEnPassant, "White pawn at d5 should be able to capture en passant on c6. Moves found: " + moves);
        }

        @Test
        @DisplayName("Black Pawn can perform en passant capture")
        void blackPawnEnPassant() {
            Pawn blackPawn = new Pawn(PieceColor.BLACK);
            board.setPiece(4, 3, blackPawn); // d4

            Pawn whitePawnOriginal = new Pawn(PieceColor.WHITE);

            Square whitePawnEndSquare = board.getSquare(4, 2);
            board.setPiece(whitePawnEndSquare.getRow(), whitePawnEndSquare.getCol(), whitePawnOriginal);

            Move whiteLastMove = new Move(new Square(6, 2), whitePawnEndSquare, whitePawnOriginal, false); // c2-c4
            game.getMoveHistory().add(whiteLastMove);

            List<Move> moves = blackPawn.getPseudoLegalMoves(game, 4, 3);

            boolean foundEnPassant = moves.stream().anyMatch(m -> m.isEnPassantMove() && m.getStartSquare().getRow() == 4 && m.getStartSquare().getCol() == 3 && // d4
                    m.getEndSquare().getRow() == 5 && m.getEndSquare().getCol() == 2 &&   // c5
                    m.getPieceCaptured() == whitePawnOriginal && m.getEnPassantCaptureSquare() == whitePawnEndSquare && m.getEnPassantCaptureSquare().getPiece() == whitePawnOriginal);
            assertTrue(foundEnPassant, "Black pawn at d4 should be able to capture en passant on c5. Moves found: " + moves);
        }
    }
}
