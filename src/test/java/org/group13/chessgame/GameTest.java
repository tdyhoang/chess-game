package org.group13.chessgame;

import org.group13.chessgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class GameTest {
    private Game game;
    private Board board;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.initializeGame();
        board = game.getBoard();
    }

    private Optional<Move> findMove(List<Move> moves, int startR, int startC, int endR, int endC) {
        return moves.stream().filter(m -> m.getStartSquare().getRow() == startR && m.getStartSquare().getCol() == startC && m.getEndSquare().getRow() == endR && m.getEndSquare().getCol() == endC && !m.isPromotion()).findFirst();
    }

    private Optional<Move> findPromotionMove(List<Move> moves, int startR, int startC, int endR, int endC, PieceType promotionType) {
        return moves.stream().filter(m -> m.getStartSquare().getRow() == startR && m.getStartSquare().getCol() == startC && m.getEndSquare().getRow() == endR && m.getEndSquare().getCol() == endC && m.isPromotion() && m.getPromotionPieceType() == promotionType).findFirst();
    }

    @Nested
    @DisplayName("Square Attack and King Check Tests")
    class AttackAndCheckTests {

        @Test
        @DisplayName("isSquareAttackedBy - Pawn attack")
        void testIsSquareAttackedByPawn() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(6, 3, new Pawn(PieceColor.WHITE))), PieceColor.WHITE);
            assertTrue(game.isSquareAttackedBy(board.getSquare(5, 2), PieceColor.WHITE), "c3 should be attacked by White Pawn at d2");
            assertTrue(game.isSquareAttackedBy(board.getSquare(5, 4), PieceColor.WHITE), "e3 should be attacked by White Pawn at d2");
            assertFalse(game.isSquareAttackedBy(board.getSquare(5, 3), PieceColor.WHITE), "d3 should NOT be 'attacked' by White Pawn for check purposes");
            assertFalse(game.isSquareAttackedBy(board.getSquare(6, 2), PieceColor.WHITE), "c2 should not be attacked by White Pawn at d2");

            board.setPiece(1, 3, new Pawn(PieceColor.BLACK));
            assertTrue(game.isSquareAttackedBy(board.getSquare(2, 2), PieceColor.BLACK), "c6 should be attacked by Black Pawn at d7");
            assertTrue(game.isSquareAttackedBy(board.getSquare(2, 4), PieceColor.BLACK), "e6 should be attacked by Black Pawn at d7");
            assertFalse(game.isSquareAttackedBy(board.getSquare(2, 3), PieceColor.BLACK), "d6 should NOT be 'attacked' by Black Pawn");
        }

        @Test
        @DisplayName("isSquareAttackedBy - Rook attack")
        void testIsSquareAttackedByRook() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(3, 3, new Rook(PieceColor.WHITE))), PieceColor.WHITE);
            assertTrue(game.isSquareAttackedBy(board.getSquare(0, 3), PieceColor.WHITE), "d8 should be attacked by Rook at d5");
            assertTrue(game.isSquareAttackedBy(board.getSquare(3, 7), PieceColor.WHITE), "h5 should be attacked by Rook at d5");
            assertFalse(game.isSquareAttackedBy(board.getSquare(2, 2), PieceColor.WHITE), "c6 should NOT be attacked by Rook at d5");

            board.setPiece(3, 5, new Pawn(PieceColor.WHITE));
            assertFalse(game.isSquareAttackedBy(board.getSquare(3, 6), PieceColor.WHITE), "g5 should NOT be attacked if f5 is blocked by ally");
        }

        @Test
        @DisplayName("isSquareAttackedBy - Knight attack")
        void testIsSquareAttackedByKnight() {
            board.setPiece(3, 3, new Knight(PieceColor.WHITE));
            assertTrue(game.isSquareAttackedBy(board.getSquare(1, 2), PieceColor.WHITE), "c7 should be attacked by Knight at d5");
            assertTrue(game.isSquareAttackedBy(board.getSquare(5, 4), PieceColor.WHITE), "e3 should be attacked by Knight at d5");
            assertFalse(game.isSquareAttackedBy(board.getSquare(3, 4), PieceColor.WHITE), "e5 should NOT be attacked by Knight");
        }

        @Test
        @DisplayName("isSquareAttackedBy - Bishop attack")
        void testIsSquareAttackedByBishop() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(3, 3, new Bishop(PieceColor.WHITE))), PieceColor.WHITE);
            assertTrue(game.isSquareAttackedBy(board.getSquare(1, 1), PieceColor.WHITE), "b7 should be attacked by Bishop at d5");
            assertTrue(game.isSquareAttackedBy(board.getSquare(5, 5), PieceColor.WHITE), "f3 should be attacked by Bishop at d5");
            assertFalse(game.isSquareAttackedBy(board.getSquare(3, 4), PieceColor.WHITE), "e5 should NOT be attacked by Bishop");

            board.setPiece(2, 2, new Knight(PieceColor.WHITE));
            assertFalse(game.isSquareAttackedBy(board.getSquare(1, 1), PieceColor.WHITE), "b7 should NOT be attacked if c6 is blocked by ally");
        }

        @Test
        @DisplayName("isSquareAttackedBy - Queen attack")
        void testIsSquareAttackedByQueen() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(3, 3, new Queen(PieceColor.WHITE))), PieceColor.WHITE);
            assertTrue(game.isSquareAttackedBy(board.getSquare(0, 3), PieceColor.WHITE), "d8 should be attacked by Queen at d5");
            assertTrue(game.isSquareAttackedBy(board.getSquare(1, 1), PieceColor.WHITE), "b7 should be attacked by Queen at d5");
            assertFalse(game.isSquareAttackedBy(board.getSquare(1, 2), PieceColor.WHITE), "c7 should NOT be attacked by Queen");

            board.setPiece(3, 5, new Pawn(PieceColor.BLACK));
            assertTrue(game.isSquareAttackedBy(board.getSquare(3, 5), PieceColor.WHITE), "f5 should be attacked (can be captured)");
            assertFalse(game.isSquareAttackedBy(board.getSquare(3, 6), PieceColor.WHITE), "g5 should NOT be attacked if f5 is blocked by enemy");
        }

        @Test
        @DisplayName("isSquareAttackedBy - King attack")
        void testIsSquareAttackedByKing() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(3, 3, new King(PieceColor.WHITE))), PieceColor.WHITE);
            assertTrue(game.isSquareAttackedBy(board.getSquare(2, 2), PieceColor.WHITE), "c6 should be attacked by King at d5");
            assertTrue(game.isSquareAttackedBy(board.getSquare(4, 4), PieceColor.WHITE), "e4 should be attacked by King at d5");
            assertFalse(game.isSquareAttackedBy(board.getSquare(1, 3), PieceColor.WHITE), "d7 should NOT be attacked by King");
        }

        @Test
        @DisplayName("isKingInCheck - Simple Rook Check")
        void testKingInCheckSimpleRook() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 4, new King(PieceColor.BLACK)), new Game.PiecePlacement(0, 0, new Rook(PieceColor.WHITE))), PieceColor.BLACK);

            assertTrue(game.isKingInCheck(PieceColor.BLACK), "Black King at e8 should be in check by White Rook at a8.");
            assertFalse(game.isKingInCheck(PieceColor.WHITE), "White King (not on board) should not be in check.");
        }

        @Test
        @DisplayName("isKingInCheck - King not in check")
        void testKingNotInCheck() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 4, new King(PieceColor.BLACK)), new Game.PiecePlacement(1, 0, new Rook(PieceColor.WHITE))), PieceColor.BLACK);
            assertFalse(game.isKingInCheck(PieceColor.BLACK), "Black King at e8 should NOT be in check by White Rook at a7.");
        }

        @Test
        @DisplayName("isKingInCheck - Discovered Check (Setup)")
        void testKingInCheckDiscovered() {
            King whiteKing = new King(PieceColor.WHITE);
            whiteKing.setHasMoved(true);
            Rook whiteRook = new Rook(PieceColor.WHITE);
            whiteRook.setHasMoved(true);
            Bishop whiteBishop = new Bishop(PieceColor.WHITE);

            King blackKing = new King(PieceColor.BLACK);
            blackKing.setHasMoved(true);

            List<Game.PiecePlacement> placements = List.of(new Game.PiecePlacement(7, 4, whiteKing, true), new Game.PiecePlacement(4, 4, whiteRook, true), new Game.PiecePlacement(3, 4, whiteBishop, false), new Game.PiecePlacement(0, 4, blackKing, true));
            game.setupBoardForTest(placements, PieceColor.WHITE);

            assertFalse(game.isKingInCheck(PieceColor.BLACK), "Initially, Black King should not be in check.");
            assertEquals(Game.GameState.ACTIVE, game.getGameState(), "Initial game state should be ACTIVE.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> bishopMoveToF6 = findMove(whiteMoves, 3, 4, 2, 5);
            assertTrue(bishopMoveToF6.isPresent(), "Bishop e5 should be able to move to f6.");

            assertTrue(game.makeMove(bishopMoveToF6.get()));

            assertEquals(PieceColor.BLACK, game.getCurrentPlayer().getColor(), "It should be Black's turn.");
            assertTrue(game.isKingInCheck(PieceColor.BLACK), "Black King should be in discovered check by Rook on e4.");
            assertEquals(Game.GameState.CHECK, game.getGameState(), "Game state should be CHECK for Black king.");
        }
    }

    @Nested
    @DisplayName("Legal Move Generation Tests")
    class LegalMoveTests {

        @Test
        @DisplayName("King cannot move into check")
        void kingCannotMoveIntoCheck() {
            King whiteKing = new King(PieceColor.WHITE);
            King blackKing = new King(PieceColor.BLACK);
            blackKing.setHasMoved(true);
            Rook blackRook = new Rook(PieceColor.BLACK);

            List<Game.PiecePlacement> placements = List.of(new Game.PiecePlacement(7, 4, whiteKing, false), new Game.PiecePlacement(0, 4, blackKing, true), new Game.PiecePlacement(6, 0, blackRook, false));
            game.setupBoardForTest(placements, PieceColor.WHITE);

            List<Move> whiteLegalMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);

            assertTrue(findMove(whiteLegalMoves, 7, 4, 7, 3).isPresent(), "Ke1 should be able to move to d1.");
            assertTrue(findMove(whiteLegalMoves, 7, 4, 7, 5).isPresent(), "Ke1 should be able to move to f1.");

            assertFalse(findMove(whiteLegalMoves, 7, 4, 6, 3).isPresent(), "Ke1 should NOT be able to move to d2 (into check from Ra2).");
            assertFalse(findMove(whiteLegalMoves, 7, 4, 6, 5).isPresent(), "Ke1 should NOT be able to move to f2 (into check from Ra2).");
            assertFalse(findMove(whiteLegalMoves, 7, 4, 6, 4).isPresent(), "Ke1 should NOT be able to move to e2 (into check from Ra2).");

            long kingMovesCount = whiteLegalMoves.stream().filter(m -> m.getPieceMoved().getType() == PieceType.KING).count();
            assertEquals(2, kingMovesCount, "White King e1 should have 2 legal moves in this setup.");
        }

        @Test
        @DisplayName("Pinned piece cannot move if it exposes King to check")
        void pinnedPieceCannotMove() {
            King whiteKing = new King(PieceColor.WHITE);
            Knight whiteKnight = new Knight(PieceColor.WHITE);
            King blackKing = new King(PieceColor.BLACK);
            blackKing.setHasMoved(true);
            Rook blackRook = new Rook(PieceColor.BLACK);

            List<Game.PiecePlacement> placements = List.of(new Game.PiecePlacement(7, 4, whiteKing), new Game.PiecePlacement(7, 1, whiteKnight), new Game.PiecePlacement(0, 4, blackKing, true), new Game.PiecePlacement(7, 0, blackRook));
            game.setupBoardForTest(placements, PieceColor.WHITE);

            List<Move> whiteLegalMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);

            long knightMovesCount = whiteLegalMoves.stream().filter(m -> m.getPieceMoved().getType() == PieceType.KNIGHT && m.getStartSquare().getRow() == 7 && m.getStartSquare().getCol() == 1).count();
            assertEquals(0, knightMovesCount, "Pinned Knight at b1 should have 0 legal moves.");

            board.setPiece(6, 2, new Pawn(PieceColor.WHITE)); // Pc2
            whiteLegalMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);

            knightMovesCount = whiteLegalMoves.stream().filter(m -> m.getPieceMoved().getType() == PieceType.KNIGHT && m.getStartSquare().getRow() == 7 && m.getStartSquare().getCol() == 1).count();
            assertEquals(0, knightMovesCount, "Pinned Knight at b1 should still have 0 legal moves.");

            long pawnMovesCount = whiteLegalMoves.stream().filter(m -> m.getPieceMoved().getType() == PieceType.PAWN && m.getStartSquare().getRow() == 6 && m.getStartSquare().getCol() == 2).count();
            assertTrue(pawnMovesCount > 0, "Unpinned Pawn c2 should have moves.");
        }

        @Test
        @DisplayName("Piece can move along the pin line if it still blocks check")
        void pieceCanMoveAlongPinLine() {
            King whiteKing = new King(PieceColor.WHITE);
            Bishop whiteBishop = new Bishop(PieceColor.WHITE);
            King blackKing = new King(PieceColor.BLACK);
            blackKing.setHasMoved(true);
            Bishop blackBishop = new Bishop(PieceColor.BLACK);

            List<Game.PiecePlacement> placements = List.of(new Game.PiecePlacement(7, 0, whiteKing), new Game.PiecePlacement(4, 3, whiteBishop), new Game.PiecePlacement(0, 4, blackKing, true), new Game.PiecePlacement(0, 7, blackBishop));
            game.setupBoardForTest(placements, PieceColor.WHITE);

            List<Move> whiteLegalMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);

            List<Move> bishopMoves = whiteLegalMoves.stream().filter(m -> m.getPieceMoved().getType() == PieceType.BISHOP && m.getStartSquare().getRow() == 4 && m.getStartSquare().getCol() == 3).collect(Collectors.toList());

            assertTrue(findMove(bishopMoves, 4, 3, 5, 2).isPresent(), "Bd4 should be able to move to c3.");
            assertTrue(findMove(bishopMoves, 4, 3, 6, 1).isPresent(), "Bd4 should be able to move to b2.");
            assertTrue(findMove(bishopMoves, 4, 3, 3, 4).isPresent(), "Bd4 should be able to move to e5.");
            assertTrue(findMove(bishopMoves, 4, 3, 2, 5).isPresent(), "Bd4 should be able to move to f6.");
            assertTrue(findMove(bishopMoves, 4, 3, 0, 7).isPresent(), "Bd4 should be able to move to h8.");

            assertFalse(findMove(bishopMoves, 4, 3, 3, 2).isPresent(), "Bd4 should NOT move to c5 (off pin line to a7).");
            assertFalse(findMove(bishopMoves, 4, 3, 5, 4).isPresent(), "Bd4 should NOT move to e3 (off pin line to h0).");

            assertEquals(6, bishopMoves.size(), "Pinned Bishop d4 should have 6 moves along the pin line a1-h8.");
        }
    }

    @Nested
    @DisplayName("Game State Tests - Checkmate and Stalemate")
    class GameStateEndConditionTests {

        @Test
        @DisplayName("Fool's Mate (Checkmate)")
        void foolsMate() {
            game.initializeGame();
            assertTrue(game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 5, 5, 5).get())); // 1. f2-f3
            assertTrue(game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 1, 4, 3, 4).get())); // 1... e7-e5
            assertTrue(game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 6, 4, 6).get())); // 2. g2-g4

            List<Move> blackMoves = game.getAllLegalMovesForPlayer(PieceColor.BLACK);
            Optional<Move> qh4 = findMove(blackMoves, 0, 3, 4, 7);
            assertTrue(qh4.isPresent(), "Black Queen should be able to move to h4.");

            assertTrue(game.makeMove(qh4.get())); // 2... Qd8-h4#

            assertEquals(Game.GameState.BLACK_WINS_CHECKMATE, game.getGameState());
            assertTrue(game.getAllLegalMovesForPlayer(PieceColor.WHITE).isEmpty(), "White should have no legal moves after checkmate.");
        }

        @Test
        @DisplayName("Simple Stalemate")
        void simpleStalemate() {
            King blackKing = new King(PieceColor.BLACK);
            King whiteKing = new King(PieceColor.WHITE);
            whiteKing.setHasMoved(true);
            Queen whiteQueen = new Queen(PieceColor.WHITE);

            List<Game.PiecePlacement> placements = List.of(new Game.PiecePlacement(0, 0, blackKing), new Game.PiecePlacement(2, 0, whiteKing, true), new Game.PiecePlacement(1, 3, whiteQueen));
            game.setupBoardForTest(placements, PieceColor.WHITE);

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> whiteQueenMoveToC7 = findMove(whiteMoves, 1, 3, 1, 2);
            assertTrue(whiteQueenMoveToC7.isPresent(), "White Queen should be able to move from d7 to c7.");

            assertTrue(game.makeMove(whiteQueenMoveToC7.get()));

            assertEquals(PieceColor.BLACK, game.getCurrentPlayer().getColor(), "It should be Black's turn.");
            assertFalse(game.isKingInCheck(PieceColor.BLACK), "Black King should NOT be in check for stalemate.");

            List<Move> blackLegalMoves = game.getAllLegalMovesForPlayer(PieceColor.BLACK);
            assertTrue(blackLegalMoves.isEmpty(), "Black should have no legal moves in stalemate.");

            assertEquals(Game.GameState.STALEMATE_DRAW, game.getGameState(), "Game state should be STALEMATE_DRAW.");
        }
    }

    // src/test/java/your/package/name/GameTest.java
// ... (imports và các phần đã có)

    @Nested
    @DisplayName("Castling Tests")
    class CastlingTests {

        private void setupInitialKingsAndRooks() {
            King whiteKing = new King(PieceColor.WHITE);
            Rook whiteRooka1 = new Rook(PieceColor.WHITE);
            Rook whiteRookh1 = new Rook(PieceColor.WHITE);
            King blackKing = new King(PieceColor.BLACK);
            Rook blackRooka8 = new Rook(PieceColor.BLACK);
            Rook blackRookh8 = new Rook(PieceColor.BLACK);

            List<Game.PiecePlacement> placements = List.of(new Game.PiecePlacement(7, 4, whiteKing), new Game.PiecePlacement(7, 0, whiteRooka1), new Game.PiecePlacement(7, 7, whiteRookh1), new Game.PiecePlacement(0, 4, blackKing), new Game.PiecePlacement(0, 0, blackRooka8), new Game.PiecePlacement(0, 7, blackRookh8));
            game.setupBoardForTest(placements, PieceColor.WHITE);
        }

        @Test
        @DisplayName("White Kingside Castling (O-O) is legal when conditions are met")
        void whiteKingsideCastlingLegal() {
            setupInitialKingsAndRooks();

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove)
                    .filter(m -> m.getEndSquare().getCol() == 6)
                    .findFirst();
            assertTrue(ooMove.isPresent(), "White Kingside Castling (O-O) should be a legal move.");

            Move castling = ooMove.get();
            assertEquals(board.getSquare(7,4), castling.getStartSquare(), "King should start at e1");
            assertEquals(board.getSquare(7,6), castling.getEndSquare(), "King should end at g1");
            assertEquals(board.getSquare(7,7), castling.getRookStartSquareForCastling(), "Rook should start at h1");
            assertEquals(board.getSquare(7,5), castling.getRookEndSquareForCastling(), "Rook should end at f1");

            assertTrue(game.makeMove(castling));
            assertSame(board.getPiece(7,6).getType(), PieceType.KING, "King should be on g1 after O-O.");
            assertSame(board.getPiece(7,5).getType(), PieceType.ROOK, "Rook should be on f1 after O-O.");
            assertTrue(board.getPiece(7,6).hasMoved(), "King should be marked as moved after O-O.");
            assertTrue(board.getPiece(7,5).hasMoved(), "Rook should be marked as moved after O-O.");
            assertNull(board.getPiece(7,4), "e1 should be empty.");
            assertNull(board.getPiece(7,7), "h1 should be empty.");
        }

        @Test
        @DisplayName("White Queenside Castling (O-O-O) is legal when conditions are met")
        void whiteQueensideCastlingLegal() {
            setupInitialKingsAndRooks();

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> oooMove = whiteMoves.stream().filter(Move::isCastlingMove)
                    .filter(m -> m.getEndSquare().getCol() == 2)
                    .findFirst();
            assertTrue(oooMove.isPresent(), "White Queenside Castling (O-O-O) should be a legal move.");

            Move castling = oooMove.get();
            assertEquals(board.getSquare(7,0), castling.getRookStartSquareForCastling(), "Rook should start at a1");
            assertEquals(board.getSquare(7,3), castling.getRookEndSquareForCastling(), "Rook should end at d1");


            assertTrue(game.makeMove(castling));
            assertSame(board.getPiece(7,2).getType(), PieceType.KING, "King should be on c1 after O-O-O.");
            assertSame(board.getPiece(7,3).getType(), PieceType.ROOK, "Rook should be on d1 after O-O-O.");
            assertTrue(board.getPiece(7,2).hasMoved());
            assertTrue(board.getPiece(7,3).hasMoved());
        }


        @Test
        @DisplayName("Cannot castle if King has moved")
        void cannotCastleIfKingMoved() {
            setupInitialKingsAndRooks();
            King whiteKing = (King) board.getPiece(7,4);
            whiteKing.setHasMoved(true);

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            assertFalse(whiteMoves.stream().anyMatch(Move::isCastlingMove), "Should not be able to castle if King has moved.");
        }

        @Test
        @DisplayName("Cannot castle if Rook has moved")
        void cannotCastleIfRookMoved() {
            setupInitialKingsAndRooks();
            Rook kingsideRook = (Rook) board.getPiece(7,7);
            kingsideRook.setHasMoved(true);

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove)
                    .filter(m -> m.getEndSquare().getCol() == 6)
                    .findFirst();
            assertFalse(ooMove.isPresent(), "Should not be able to kingside castle if h1-Rook has moved.");
            assertTrue(whiteMoves.stream().filter(Move::isCastlingMove).anyMatch(m -> m.getEndSquare().getCol() == 2), "Queenside castling should still be possible.");
        }

        @Test
        @DisplayName("Cannot castle if path is blocked (Kingside)")
        void cannotCastleIfPathBlockedKingside() {
            setupInitialKingsAndRooks();
            board.setPiece(7,5, new Knight(PieceColor.WHITE));

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove)
                    .filter(m -> m.getEndSquare().getCol() == 6)
                    .findFirst();
            assertFalse(ooMove.isPresent(), "Should not kingside castle if f1 is blocked.");
        }

        @Test
        @DisplayName("Cannot castle if King is in check")
        void cannotCastleIfKingInCheck() {
            setupInitialKingsAndRooks();
            board.setPiece(6,4, new Rook(PieceColor.BLACK));

            assertTrue(game.isKingInCheck(PieceColor.WHITE), "White King should be in check.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            assertFalse(whiteMoves.stream().anyMatch(Move::isCastlingMove), "Should not be able to castle if King is in check.");
        }

        @Test
        @DisplayName("Cannot castle through an attacked square (Kingside f1)")
        void cannotCastleThroughAttackedSquareKingside() {
            setupInitialKingsAndRooks();
            board.setPiece(0,5, new Rook(PieceColor.BLACK));

            assertTrue(game.isSquareAttackedBy(board.getSquare(7,5), PieceColor.BLACK), "f1 should be attacked by Black Rook at f8.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove)
                    .filter(m -> m.getEndSquare().getCol() == 6)
                    .findFirst();
            assertFalse(ooMove.isPresent(), "Should not kingside castle if f1 (square King passes through) is attacked.");
        }

        @Test
        @DisplayName("Cannot castle into an attacked square (Kingside g1)")
        void cannotCastleIntoAttackedSquareKingside() {
            setupInitialKingsAndRooks();
            board.setPiece(0,6, new Rook(PieceColor.BLACK));

            assertTrue(game.isSquareAttackedBy(board.getSquare(7,6), PieceColor.BLACK), "g1 should be attacked by Black Rook at g8.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove)
                    .filter(m -> m.getEndSquare().getCol() == 6)
                    .findFirst();
            assertFalse(ooMove.isPresent(), "Should not kingside castle if g1 (square King lands on) is attacked.");
        }

        @Test
        @DisplayName("Undo Castling (Kingside)")
        void undoCastlingKingside() {
            setupInitialKingsAndRooks();

            Move ooMove = game.getAllLegalMovesForPlayer(PieceColor.WHITE).stream()
                    .filter(Move::isCastlingMove)
                    .filter(m -> m.getEndSquare().getCol() == 6)
                    .findFirst().orElseThrow(() -> new AssertionError("Kingside castling move not found"));

            Piece originalKing = board.getPiece(7,4);
            Piece originalRook = board.getPiece(7,7);
            assertFalse(originalKing.hasMoved());
            assertFalse(originalRook.hasMoved());

            assertTrue(game.makeMove(ooMove));

            game.undoLastMove();

            assertSame(originalKing, board.getPiece(7,4), "King should be back on e1.");
            assertSame(originalRook, board.getPiece(7,7), "Rook should be back on h1.");
            assertNull(board.getPiece(7,6), "g1 should be empty.");
            assertNull(board.getPiece(7,5), "f1 should be empty.");
            assertFalse(board.getPiece(7,4).hasMoved(), "King's hasMoved should be false after undo.");
            assertFalse(board.getPiece(7,7).hasMoved(), "Rook's hasMoved should be false after undo.");
            assertEquals(PieceColor.WHITE, game.getCurrentPlayer().getColor(), "Should be White's turn again after undoing White's move.");
        }

    }
}
