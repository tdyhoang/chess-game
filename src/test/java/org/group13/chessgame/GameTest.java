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

        @Test
        @DisplayName("Back Rank Mate")
        void backRankMate() {
            // White: Ke1, Ra8. Black: Kg8, Pawns f7,g7,h7
            game.setupBoardForTest(List.of(new Game.PiecePlacement(7, 4, new King(PieceColor.WHITE)), new Game.PiecePlacement(0, 0, new Rook(PieceColor.WHITE)), new Game.PiecePlacement(0, 6, new King(PieceColor.BLACK)), new Game.PiecePlacement(1, 5, new Pawn(PieceColor.BLACK)), new Game.PiecePlacement(1, 6, new Pawn(PieceColor.BLACK)), new Game.PiecePlacement(1, 7, new Pawn(PieceColor.BLACK))), PieceColor.WHITE);

            Move ra8e8 = findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 0, 0, 0, 4).orElseThrow(() -> new AssertionError("Move Ra8-e8 not found."));
            assertTrue(game.makeMove(ra8e8));

            assertEquals(Game.GameState.WHITE_WINS_CHECKMATE, game.getGameState());
            assertTrue(game.getAllLegalMovesForPlayer(PieceColor.BLACK).isEmpty());
        }

        @Test
        @DisplayName("Queen and King vs King Checkmate")
        void queenKingVsKingCheckmate() {
            // White: Kc1, Qc2. Black: Ka1
            game.setupBoardForTest(List.of(new Game.PiecePlacement(7, 2, new King(PieceColor.WHITE)), new Game.PiecePlacement(6, 2, new Queen(PieceColor.WHITE)), new Game.PiecePlacement(7, 0, new King(PieceColor.BLACK))), PieceColor.WHITE);

            Move qc2b2 = findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 2, 6, 1).orElseThrow(() -> new AssertionError("Move Qc2-b2 not found."));
            assertTrue(game.makeMove(qc2b2));

            assertEquals(Game.GameState.WHITE_WINS_CHECKMATE, game.getGameState());
        }
    }

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
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 6).findFirst();
            assertTrue(ooMove.isPresent(), "White Kingside Castling (O-O) should be a legal move.");

            Move castling = ooMove.get();
            assertEquals(board.getSquare(7, 4), castling.getStartSquare(), "King should start at e1");
            assertEquals(board.getSquare(7, 6), castling.getEndSquare(), "King should end at g1");
            assertEquals(board.getSquare(7, 7), castling.getRookStartSquareForCastling(), "Rook should start at h1");
            assertEquals(board.getSquare(7, 5), castling.getRookEndSquareForCastling(), "Rook should end at f1");

            assertTrue(game.makeMove(castling));
            assertSame(board.getPiece(7, 6).getType(), PieceType.KING, "King should be on g1 after O-O.");
            assertSame(board.getPiece(7, 5).getType(), PieceType.ROOK, "Rook should be on f1 after O-O.");
            assertTrue(board.getPiece(7, 6).hasMoved(), "King should be marked as moved after O-O.");
            assertTrue(board.getPiece(7, 5).hasMoved(), "Rook should be marked as moved after O-O.");
            assertNull(board.getPiece(7, 4), "e1 should be empty.");
            assertNull(board.getPiece(7, 7), "h1 should be empty.");
        }

        @Test
        @DisplayName("White Queenside Castling (O-O-O) is legal when conditions are met")
        void whiteQueensideCastlingLegal() {
            setupInitialKingsAndRooks();

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> oooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 2).findFirst();
            assertTrue(oooMove.isPresent(), "White Queenside Castling (O-O-O) should be a legal move.");

            Move castling = oooMove.get();
            assertEquals(board.getSquare(7, 0), castling.getRookStartSquareForCastling(), "Rook should start at a1");
            assertEquals(board.getSquare(7, 3), castling.getRookEndSquareForCastling(), "Rook should end at d1");


            assertTrue(game.makeMove(castling));
            assertSame(board.getPiece(7, 2).getType(), PieceType.KING, "King should be on c1 after O-O-O.");
            assertSame(board.getPiece(7, 3).getType(), PieceType.ROOK, "Rook should be on d1 after O-O-O.");
            assertTrue(board.getPiece(7, 2).hasMoved());
            assertTrue(board.getPiece(7, 3).hasMoved());
        }


        @Test
        @DisplayName("Cannot castle if King has moved")
        void cannotCastleIfKingMoved() {
            setupInitialKingsAndRooks();
            King whiteKing = (King) board.getPiece(7, 4);
            whiteKing.setHasMoved(true);

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            assertFalse(whiteMoves.stream().anyMatch(Move::isCastlingMove), "Should not be able to castle if King has moved.");
        }

        @Test
        @DisplayName("Cannot castle if Rook has moved")
        void cannotCastleIfRookMoved() {
            setupInitialKingsAndRooks();
            Rook kingsideRook = (Rook) board.getPiece(7, 7);
            kingsideRook.setHasMoved(true);

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 6).findFirst();
            assertFalse(ooMove.isPresent(), "Should not be able to kingside castle if h1-Rook has moved.");
            assertTrue(whiteMoves.stream().filter(Move::isCastlingMove).anyMatch(m -> m.getEndSquare().getCol() == 2), "Queenside castling should still be possible.");
        }

        @Test
        @DisplayName("Cannot castle if path is blocked (Kingside)")
        void cannotCastleIfPathBlockedKingside() {
            setupInitialKingsAndRooks();
            board.setPiece(7, 5, new Knight(PieceColor.WHITE));

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 6).findFirst();
            assertFalse(ooMove.isPresent(), "Should not kingside castle if f1 is blocked.");
        }

        @Test
        @DisplayName("Cannot castle if King is in check")
        void cannotCastleIfKingInCheck() {
            setupInitialKingsAndRooks();
            board.setPiece(6, 4, new Rook(PieceColor.BLACK));

            assertTrue(game.isKingInCheck(PieceColor.WHITE), "White King should be in check.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            assertFalse(whiteMoves.stream().anyMatch(Move::isCastlingMove), "Should not be able to castle if King is in check.");
        }

        @Test
        @DisplayName("Cannot castle through an attacked square (Kingside f1)")
        void cannotCastleThroughAttackedSquareKingside() {
            setupInitialKingsAndRooks();
            board.setPiece(0, 5, new Rook(PieceColor.BLACK));

            assertTrue(game.isSquareAttackedBy(board.getSquare(7, 5), PieceColor.BLACK), "f1 should be attacked by Black Rook at f8.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 6).findFirst();
            assertFalse(ooMove.isPresent(), "Should not kingside castle if f1 (square King passes through) is attacked.");
        }

        @Test
        @DisplayName("Cannot castle into an attacked square (Kingside g1)")
        void cannotCastleIntoAttackedSquareKingside() {
            setupInitialKingsAndRooks();
            board.setPiece(0, 6, new Rook(PieceColor.BLACK));

            assertTrue(game.isSquareAttackedBy(board.getSquare(7, 6), PieceColor.BLACK), "g1 should be attacked by Black Rook at g8.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> ooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 6).findFirst();
            assertFalse(ooMove.isPresent(), "Should not kingside castle if g1 (square King lands on) is attacked.");
        }

        @Test
        @DisplayName("Undo Castling (Kingside)")
        void undoCastlingKingside() {
            setupInitialKingsAndRooks();

            Move ooMove = game.getAllLegalMovesForPlayer(PieceColor.WHITE).stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 6).findFirst().orElseThrow(() -> new AssertionError("Kingside castling move not found"));

            Piece originalKing = board.getPiece(7, 4);
            Piece originalRook = board.getPiece(7, 7);
            assertFalse(originalKing.hasMoved());
            assertFalse(originalRook.hasMoved());

            assertTrue(game.makeMove(ooMove));

            game.undoLastMove();

            assertSame(originalKing, board.getPiece(7, 4), "King should be back on e1.");
            assertSame(originalRook, board.getPiece(7, 7), "Rook should be back on h1.");
            assertNull(board.getPiece(7, 6), "g1 should be empty.");
            assertNull(board.getPiece(7, 5), "f1 should be empty.");
            assertFalse(board.getPiece(7, 4).hasMoved(), "King's hasMoved should be false after undo.");
            assertFalse(board.getPiece(7, 7).hasMoved(), "Rook's hasMoved should be false after undo.");
            assertEquals(PieceColor.WHITE, game.getCurrentPlayer().getColor(), "Should be White's turn again after undoing White's move.");
        }

        @Test
        @DisplayName("Cannot castle Queenside if path (b1,c1,d1) is blocked")
        void cannotCastleIfPathBlockedQueenside() {
            setupInitialKingsAndRooks();
            board.setPiece(7, 2, new Knight(PieceColor.WHITE));

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> oooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 2).findFirst();
            assertFalse(oooMove.isPresent(), "Should not queenside castle if c1 is blocked.");
        }

        @Test
        @DisplayName("Cannot castle Queenside through an attacked square (d1)")
        void cannotCastleThroughAttackedSquareQueensideD1() {
            setupInitialKingsAndRooks();
            board.setPiece(0, 3, new Rook(PieceColor.BLACK));

            assertTrue(game.isSquareAttackedBy(board.getSquare(7, 3), PieceColor.BLACK), "d1 should be attacked by Black Rook at d8.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> oooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 2).findFirst();
            assertFalse(oooMove.isPresent(), "Should not queenside castle if d1 (square King passes through) is attacked.");
        }

        @Test
        @DisplayName("Cannot castle Queenside through an attacked square (c1 - landing)")
        void cannotCastleIntoAttackedSquareQueensideC1() {
            setupInitialKingsAndRooks();
            board.setPiece(0, 2, new Rook(PieceColor.BLACK));

            assertTrue(game.isSquareAttackedBy(board.getSquare(7, 2), PieceColor.BLACK), "c1 should be attacked by Black Rook at c8.");

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> oooMove = whiteMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 2).findFirst();
            assertFalse(oooMove.isPresent(), "Should not queenside castle if c1 (square King lands on) is attacked.");
        }

        @Test
        @DisplayName("Undo Castling (Queenside)")
        void undoCastlingQueenside() {
            setupInitialKingsAndRooks();

            Move oooMove = game.getAllLegalMovesForPlayer(PieceColor.WHITE).stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 2).findFirst().orElseThrow(() -> new AssertionError("Queenside castling move not found"));

            Piece originalKing = board.getPiece(7, 4);
            Piece originalRook = board.getPiece(7, 0);
            assertFalse(originalKing.hasMoved());
            assertFalse(originalRook.hasMoved());

            assertTrue(game.makeMove(oooMove));

            assertTrue(game.undoLastMove(), "Undo last move should be successful.");

            assertSame(originalKing, board.getPiece(7, 4), "King should be back on e1.");
            assertSame(originalRook, board.getPiece(7, 0), "Rook should be back on a1.");
            assertNull(board.getPiece(7, 2), "c1 should be empty.");
            assertNull(board.getPiece(7, 3), "d1 should be empty.");
            assertFalse(board.getPiece(7, 4).hasMoved(), "King's hasMoved should be false after undo.");
            assertFalse(board.getPiece(7, 0).hasMoved(), "Rook's hasMoved should be false after undo.");
            assertEquals(PieceColor.WHITE, game.getCurrentPlayer().getColor(), "Should be White's turn again.");
            assertEquals(Game.GameState.ACTIVE, game.getGameState());
        }

        @Test
        @DisplayName("Black Kingside Castling (O-O) is legal")
        void blackKingsideCastlingLegal() {
            setupInitialKingsAndRooks();
            game.setCurrentPlayerColorForTest(PieceColor.BLACK);

            List<Move> blackMoves = game.getAllLegalMovesForPlayer(PieceColor.BLACK);
            Optional<Move> ooMove = blackMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 6).findFirst();
            assertTrue(ooMove.isPresent(), "Black Kingside Castling (O-O) should be a legal move.");

            assertTrue(game.makeMove(ooMove.get()));
            assertSame(board.getPiece(0, 6).getType(), PieceType.KING);
            assertSame(board.getPiece(0, 5).getType(), PieceType.ROOK);
        }

        @Test
        @DisplayName("Black Queenside Castling (O-O-O) is legal")
        void blackQueensideCastlingLegal() {
            setupInitialKingsAndRooks();
            game.setCurrentPlayerColorForTest(PieceColor.BLACK);

            List<Move> blackMoves = game.getAllLegalMovesForPlayer(PieceColor.BLACK);
            Optional<Move> oooMove = blackMoves.stream().filter(Move::isCastlingMove).filter(m -> m.getEndSquare().getCol() == 2).findFirst();
            assertTrue(oooMove.isPresent(), "Black Queenside Castling (O-O-O) should be a legal move.");

            assertTrue(game.makeMove(oooMove.get()));
            assertSame(board.getPiece(0, 2).getType(), PieceType.KING);
            assertSame(board.getPiece(0, 3).getType(), PieceType.ROOK);
        }
    }

    @Nested
    @DisplayName("En Passant Tests")
    class EnPassantTests {

        @Test
        @DisplayName("White Pawn can perform en passant capture")
        void whitePawnEnPassantLegal() {
            Pawn whitePawnD5 = new Pawn(PieceColor.WHITE);
            Pawn blackPawnC7 = new Pawn(PieceColor.BLACK);

            game.setupBoardForTest(List.of(new Game.PiecePlacement(3, 3, whitePawnD5), new Game.PiecePlacement(1, 2, blackPawnC7)), PieceColor.BLACK);

            Move blackPawnMove = findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 1, 2, 3, 2).orElseThrow(() -> new AssertionError("Black pawn c7-c5 move not found"));
            assertTrue(game.makeMove(blackPawnMove));

            assertSame(game.getCurrentPlayer().getColor(), PieceColor.WHITE);
            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);

            Optional<Move> enPassantMove = whiteMoves.stream().filter(Move::isEnPassantMove).filter(m -> m.getStartSquare().getRow() == 3 && m.getStartSquare().getCol() == 3).filter(m -> m.getEndSquare().getRow() == 2 && m.getEndSquare().getCol() == 2).findFirst();

            assertTrue(enPassantMove.isPresent(), "White pawn d5 should have en passant capture to c6.");

            Move epMove = enPassantMove.get();
            assertNotNull(epMove.getPieceCaptured(), "En passant move should have a captured piece.");
            assertSame(blackPawnC7, epMove.getPieceCaptured(), "Captured piece should be the black pawn from c5.");
            assertNotNull(epMove.getEnPassantCaptureSquare(), "En passant move should specify the capture square.");
            assertEquals(board.getSquare(3, 2), epMove.getEnPassantCaptureSquare(), "En passant capture square should be c5.");


            assertTrue(game.makeMove(epMove));
            assertSame(whitePawnD5, board.getPiece(2, 2), "White pawn should be at c6 after e.p.");
            assertNull(board.getPiece(3, 3), "d5 should be empty.");
            assertNull(board.getPiece(3, 2), "c5 (black pawn's original square after 2-step) should be empty after e.p. capture.");
        }

        @Test
        @DisplayName("En passant is not legal if not immediately after opponent's two-square pawn move")
        void enPassantNotLegalIfNotImmediate() {
            Pawn whitePawnD5 = new Pawn(PieceColor.WHITE);
            Pawn blackPawnC7 = new Pawn(PieceColor.BLACK);
            Pawn whitePawnH2 = new Pawn(PieceColor.WHITE);
            Pawn blackPawnA7 = new Pawn(PieceColor.BLACK);


            List<Game.PiecePlacement> placements = List.of(new Game.PiecePlacement(3, 3, whitePawnD5), new Game.PiecePlacement(1, 2, blackPawnC7), new Game.PiecePlacement(6, 7, whitePawnH2), new Game.PiecePlacement(1, 0, blackPawnA7));
            game.setupBoardForTest(placements, PieceColor.BLACK);

            Move blackPawnMove1 = findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 1, 2, 3, 2).get(); // c5
            assertTrue(game.makeMove(blackPawnMove1));

            Move whiteIntermediateMove = findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 7, 5, 7).get(); // h3
            assertTrue(game.makeMove(whiteIntermediateMove));

            Move blackIntermediateMove = findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 1, 0, 2, 0).get(); // a6
            assertTrue(game.makeMove(blackIntermediateMove));

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> enPassantMove = whiteMoves.stream().filter(Move::isEnPassantMove).filter(m -> m.getStartSquare().getRow() == 3 && m.getStartSquare().getCol() == 3).findFirst();

            assertFalse(enPassantMove.isPresent(), "En passant should not be legal after intermediate moves.");
        }

        @Test
        @DisplayName("En passant not legal if opponent pawn moved one square")
        void enPassantNotLegalIfOpponentOneSquareMove() {
            Pawn whitePawnD5 = new Pawn(PieceColor.WHITE);
            Pawn blackPawnC6 = new Pawn(PieceColor.BLACK);
            game.setupBoardForTest(List.of(new Game.PiecePlacement(3, 3, whitePawnD5), new Game.PiecePlacement(2, 2, blackPawnC6)), PieceColor.BLACK);

            Move blackPawnMove = findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 2, 2, 3, 2).get(); // c5
            assertTrue(game.makeMove(blackPawnMove));

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> enPassantMove = whiteMoves.stream().filter(Move::isEnPassantMove).filter(m -> m.getStartSquare().getRow() == 3 && m.getStartSquare().getCol() == 3).findFirst();
            assertFalse(enPassantMove.isPresent(), "En passant not legal if opponent pawn moved one square.");
        }

        @Test
        @DisplayName("Undo En Passant capture")
        void undoEnPassantCapture() {
            Pawn whitePawnD5 = new Pawn(PieceColor.WHITE);
            Pawn blackPawnC7 = new Pawn(PieceColor.BLACK);
            game.setupBoardForTest(List.of(new Game.PiecePlacement(3, 3, whitePawnD5), new Game.PiecePlacement(1, 2, blackPawnC7)), PieceColor.BLACK);

            Move blackPawnMoveToC5 = findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 1, 2, 3, 2).get();
            assertTrue(game.makeMove(blackPawnMoveToC5));

            Move enPassantCapture = game.getAllLegalMovesForPlayer(PieceColor.WHITE).stream().filter(Move::isEnPassantMove).filter(m -> m.getStartSquare().getRow() == 3 && m.getStartSquare().getCol() == 3).findFirst().orElseThrow(() -> new AssertionError("En passant move not found for White."));

            assertTrue(game.makeMove(enPassantCapture));

            assertTrue(game.undoLastMove(), "Undo en passant capture should be successful.");

            assertSame(whitePawnD5, board.getPiece(3, 3), "White pawn should be back on d5.");
            assertSame(blackPawnC7, board.getPiece(3, 2), "Captured Black pawn should be back on c5.");
            assertNull(board.getPiece(2, 2), "c6 (en passant target square) should be empty.");

            assertEquals(PieceColor.WHITE, game.getCurrentPlayer().getColor(), "Should be White's turn again (before the e.p. move).");
            assertEquals(blackPawnMoveToC5, game.getLastMove(), "Last move in history should be Black's c7-c5.");
        }

        @Test
        @DisplayName("Black Pawn can perform en passant capture")
        void blackPawnEnPassantLegal() {
            Pawn blackPawnD4 = new Pawn(PieceColor.BLACK);
            Pawn whitePawnC2 = new Pawn(PieceColor.WHITE);
            game.setupBoardForTest(List.of(new Game.PiecePlacement(4, 3, blackPawnD4), new Game.PiecePlacement(6, 2, whitePawnC2)), PieceColor.WHITE);

            Move whitePawnMove = findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 2, 4, 2).orElseThrow(() -> new AssertionError("White pawn c2-c4 move not found"));
            assertTrue(game.makeMove(whitePawnMove));

            assertSame(game.getCurrentPlayer().getColor(), PieceColor.BLACK);
            List<Move> blackMoves = game.getAllLegalMovesForPlayer(PieceColor.BLACK);

            Optional<Move> enPassantMove = blackMoves.stream().filter(Move::isEnPassantMove).filter(m -> m.getStartSquare().getRow() == 4 && m.getStartSquare().getCol() == 3).filter(m -> m.getEndSquare().getRow() == 5 && m.getEndSquare().getCol() == 2).findFirst();

            assertTrue(enPassantMove.isPresent(), "Black pawn d4 should have en passant capture to c3.");

            Move epMove = enPassantMove.get();
            assertSame(whitePawnC2, epMove.getPieceCaptured(), "Captured piece should be the white pawn from c4.");
            assertEquals(board.getSquare(4, 2), epMove.getEnPassantCaptureSquare(), "En passant capture square should be c4.");

            assertTrue(game.makeMove(epMove));
            assertSame(blackPawnD4, board.getPiece(5, 2), "Black pawn should be at c3 after e.p.");
            assertNull(board.getPiece(4, 3), "d4 should be empty.");
            assertNull(board.getPiece(4, 2), "c4 (white pawn's original square after 2-step) should be empty after e.p. capture.");
        }

        @Test
        @DisplayName("En passant is correctly invalidated by a subsequent move")
        void enPassantInvalidatedByNextPly() {
            Pawn whitePawnE5 = new Pawn(PieceColor.WHITE);
            Pawn blackPawnD7 = new Pawn(PieceColor.BLACK);
            King whiteKingE1 = new King(PieceColor.WHITE);
            King blackKingE8 = new King(PieceColor.BLACK);

            List<Game.PiecePlacement> placements = List.of(new Game.PiecePlacement(3, 4, whitePawnE5), new Game.PiecePlacement(1, 3, blackPawnD7), new Game.PiecePlacement(7, 4, whiteKingE1), new Game.PiecePlacement(0, 4, blackKingE8));
            game.setupBoardForTest(placements, PieceColor.BLACK);

            Move blackPawnD7D5 = findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 1, 3, 3, 3).get();
            assertTrue(game.makeMove(blackPawnD7D5));

            Move whiteKingE1E2 = findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 7, 4, 6, 4).get();
            assertTrue(game.makeMove(whiteKingE1E2));

            Move blackKingE8E7 = findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 0, 4, 1, 4).get();
            assertTrue(game.makeMove(blackKingE8E7));

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            boolean enPassantAvailable = whiteMoves.stream().anyMatch(m -> m.isEnPassantMove() && m.getStartSquare().getPiece() == whitePawnE5 && m.getEndSquare().getRow() == 2 && m.getEndSquare().getCol() == 3);

            assertFalse(enPassantAvailable, "En passant e5xd6 should not be available after intermediate moves.");
        }
    }

    @Nested
    @DisplayName("Pawn Promotion Tests")
    class PromotionTests {

        @Test
        @DisplayName("White Pawn promotes to Queen (no capture)")
        void whitePawnPromotesToQueenNoCapture() {
            Pawn whitePawnA7 = new Pawn(PieceColor.WHITE);
            whitePawnA7.setHasMoved(true);
            game.setupBoardForTest(List.of(new Game.PiecePlacement(1, 0, whitePawnA7, true)), PieceColor.WHITE);

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> promotionToQueenMove = findPromotionMove(whiteMoves, 1, 0, 0, 0, PieceType.QUEEN);

            assertTrue(promotionToQueenMove.isPresent(), "Promotion to Queen move a7-a8=Q should be available.");

            assertTrue(game.makeMove(promotionToQueenMove.get()));

            Piece pieceAtA8 = board.getPiece(0, 0);
            assertNotNull(pieceAtA8);
            assertEquals(PieceType.QUEEN, pieceAtA8.getType(), "Piece at a8 should be a Queen.");
            assertEquals(PieceColor.WHITE, pieceAtA8.getColor());
            assertTrue(pieceAtA8.hasMoved(), "Promoted Queen should be marked as moved.");
            assertNull(board.getPiece(1, 0), "a7 should be empty.");
        }

        @Test
        @DisplayName("White Pawn promotes to Knight with capture")
        void whitePawnPromotesToKnightWithCapture() {
            Pawn whitePawnB7 = new Pawn(PieceColor.WHITE);
            Rook blackRookC8 = new Rook(PieceColor.BLACK);
            whitePawnB7.setHasMoved(true);
            game.setupBoardForTest(List.of(new Game.PiecePlacement(1, 1, whitePawnB7, true), new Game.PiecePlacement(0, 2, blackRookC8)), PieceColor.WHITE);

            List<Move> whiteMoves = game.getAllLegalMovesForPlayer(PieceColor.WHITE);
            Optional<Move> promotionToKnightMove = findPromotionMove(whiteMoves, 1, 1, 0, 2, PieceType.KNIGHT);

            assertTrue(promotionToKnightMove.isPresent(), "Promotion to Knight move b7xc8=N should be available.");
            assertSame(blackRookC8, promotionToKnightMove.get().getPieceCaptured(), "Captured piece should be the black rook.");

            assertTrue(game.makeMove(promotionToKnightMove.get()));

            Piece pieceAtC8 = board.getPiece(0, 2);
            assertNotNull(pieceAtC8);
            assertEquals(PieceType.KNIGHT, pieceAtC8.getType(), "Piece at c8 should be a Knight.");
            assertEquals(PieceColor.WHITE, pieceAtC8.getColor());
            assertNull(board.getPiece(1, 1), "b7 should be empty.");
        }

        @Test
        @DisplayName("Undo Pawn Promotion (no capture)")
        void undoPawnPromotionNoCapture() {
            Pawn whitePawnA7 = new Pawn(PieceColor.WHITE);
            whitePawnA7.setHasMoved(true);
            game.setupBoardForTest(List.of(new Game.PiecePlacement(1, 0, whitePawnA7, true)), PieceColor.WHITE);

            Move promotionMove = findPromotionMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 1, 0, 0, 0, PieceType.QUEEN).get();
            assertTrue(game.makeMove(promotionMove));

            assertTrue(game.undoLastMove());

            Piece pieceAtA7 = board.getPiece(1, 0);
            assertNotNull(pieceAtA7);
            assertEquals(PieceType.PAWN, pieceAtA7.getType(), "Should be a Pawn back at a7.");
            // assertSame(whitePawnA7, pieceAtA7);
            assertEquals(PieceColor.WHITE, pieceAtA7.getColor());
            assertTrue(pieceAtA7.hasMoved(), "Pawn's original hasMoved status should be restored.");
            assertNull(board.getPiece(0, 0), "a8 should be empty after undo.");
            assertEquals(PieceColor.WHITE, game.getCurrentPlayer().getColor());
        }

        @Test
        @DisplayName("Black Pawn promotes to Rook")
        void blackPawnPromotesToRook() {
            Pawn blackPawnH2 = new Pawn(PieceColor.BLACK);
            blackPawnH2.setHasMoved(true);
            game.setupBoardForTest(List.of(new Game.PiecePlacement(6, 7, blackPawnH2, true)), PieceColor.BLACK);

            List<Move> blackMoves = game.getAllLegalMovesForPlayer(PieceColor.BLACK);
            Optional<Move> promotionToRookMove = findPromotionMove(blackMoves, 6, 7, 7, 7, PieceType.ROOK);

            assertTrue(promotionToRookMove.isPresent(), "Promotion to Rook move h2-h1=R should be available.");

            assertTrue(game.makeMove(promotionToRookMove.get()));

            Piece pieceAtH1 = board.getPiece(7, 7);
            assertNotNull(pieceAtH1);
            assertEquals(PieceType.ROOK, pieceAtH1.getType());
            assertEquals(PieceColor.BLACK, pieceAtH1.getColor());
        }
    }

    @Nested
    @DisplayName("Fifty-Move Rule Tests")
    class FiftyMoveRuleTests {

        @Test
        @DisplayName("Game is drawn after 50 moves (100 plies) without pawn move or capture")
        void testFiftyMoveDraw() {
            // White: Ka1, Qb2. Black: Kc8
            game.setupBoardForTest(List.of(new Game.PiecePlacement(7, 0, new King(PieceColor.WHITE)), new Game.PiecePlacement(6, 1, new Queen(PieceColor.WHITE)), new Game.PiecePlacement(0, 2, new King(PieceColor.BLACK))), PieceColor.WHITE);
            assertEquals(0, game.getHalfMoveClock());

            for (int i = 0; i < 99; i++) {
                if (i % 2 == 0) {
                    game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 1, 6, 2).orElseGet(() -> findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 2, 6, 1).get())); // Qc2 or Qb2
                } else {
                    game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 0, 2, 0, 3).orElseGet(() -> findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 0, 3, 0, 2).get())); // Kd8 or Kc8
                }
                if (game.getGameState() != Game.GameState.ACTIVE && game.getGameState() != Game.GameState.CHECK) break;
            }
            if (game.getHalfMoveClock() == 99) {
                assertTrue(game.getGameState() == Game.GameState.ACTIVE || game.getGameState() == Game.GameState.CHECK);
                if (game.getCurrentPlayer().getColor() == PieceColor.WHITE) {
                    game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 1, 6, 2).orElseGet(() -> findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 2, 6, 1).get())); // Qc2 or Qb2
                } else {
                    game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 0, 2, 0, 3).orElseGet(() -> findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 0, 3, 0, 2).get())); // Kd8 or Kc8
                }
                assertEquals(100, game.getHalfMoveClock());
                assertEquals(Game.GameState.FIFTY_MOVE_DRAW, game.getGameState());
            } else {
                System.out.println("FiftyMoveRuleTest: Game ended before 99 half-moves. Current clock: " + game.getHalfMoveClock() + ", State: " + game.getGameState());
            }
        }

        @Test
        @DisplayName("Half-move clock resets after a pawn move")
        void testHalfMoveClockResetsOnPawnMove() {
            game.initializeGame();

            game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 7, 6, 5, 5).get()); // Nf3
            assertEquals(1, game.getHalfMoveClock());
            game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 0, 1, 2, 2).get()); // Nc6
            assertEquals(2, game.getHalfMoveClock());

            game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 4, 4, 4).get()); // e4
            assertEquals(0, game.getHalfMoveClock(), "Half-move clock should reset to 0 after a pawn move.");
        }

        @Test
        @DisplayName("Half-move clock resets after a capture")
        void testHalfMoveClockResetsOnCapture() {
            game.initializeGame();
            game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 6, 3, 4, 3).get()); // d4
            assertEquals(0, game.getHalfMoveClock());
            game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 1, 3, 3, 3).get()); // d5
            assertEquals(0, game.getHalfMoveClock());
            game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 7, 1, 5, 2).get()); // Nc3
            assertEquals(1, game.getHalfMoveClock());
            game.makeMove(findMove(game.getAllLegalMovesForPlayer(PieceColor.BLACK), 0, 6, 2, 5).get()); // Nf6
            assertEquals(2, game.getHalfMoveClock());

            Move captureMove = findMove(game.getAllLegalMovesForPlayer(PieceColor.WHITE), 5, 2, 3, 3).orElseThrow(() -> new AssertionError("Capture move Nxd5 not found"));
            assertNotNull(captureMove.getPieceCaptured());
            assertTrue(game.makeMove(captureMove));
            assertEquals(0, game.getHalfMoveClock(), "Half-move clock should reset to 0 after a capture.");
        }
    }

    @Nested
    @DisplayName("Insufficient Material Tests")
    class InsufficientMaterialTests {
        @Test
        @DisplayName("King vs King is a draw")
        void kingVsKing() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 0, new King(PieceColor.WHITE)), new Game.PiecePlacement(7, 7, new King(PieceColor.BLACK))), PieceColor.WHITE);
            game._test_triggerUpdateGameState();
            assertEquals(Game.GameState.INSUFFICIENT_MATERIAL_DRAW, game.getGameState());
        }

        @Test
        @DisplayName("King vs King and Knight is a draw")
        void kingVsKingAndKnight() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 0, new King(PieceColor.WHITE)), new Game.PiecePlacement(7, 7, new King(PieceColor.BLACK)), new Game.PiecePlacement(7, 6, new Knight(PieceColor.BLACK))), PieceColor.WHITE);
            game._test_triggerUpdateGameState();
            assertEquals(Game.GameState.INSUFFICIENT_MATERIAL_DRAW, game.getGameState());
        }

        @Test
        @DisplayName("King vs King and Bishop is a draw")
        void kingVsKingAndBishop() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 0, new King(PieceColor.WHITE)), new Game.PiecePlacement(7, 7, new King(PieceColor.BLACK)), new Game.PiecePlacement(7, 5, new Bishop(PieceColor.BLACK))), PieceColor.WHITE);
            game._test_triggerUpdateGameState();
            assertEquals(Game.GameState.INSUFFICIENT_MATERIAL_DRAW, game.getGameState());
        }

        @Test
        @DisplayName("King and Bishop vs King and Bishop (same color squares) is a draw")
        void kingAndBishopVsKingAndBishopSameColor() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 0, new King(PieceColor.WHITE)), new Game.PiecePlacement(0, 1, new Bishop(PieceColor.WHITE)), new Game.PiecePlacement(7, 7, new King(PieceColor.BLACK)), new Game.PiecePlacement(7, 6, new Bishop(PieceColor.BLACK))), PieceColor.WHITE);
            game._test_triggerUpdateGameState();
            assertEquals(Game.GameState.INSUFFICIENT_MATERIAL_DRAW, game.getGameState());
        }

        @Test
        @DisplayName("King and Bishop vs King and Bishop (different color squares) is NOT necessarily a draw by this rule")
        void kingAndBishopVsKingAndBishopDifferentColor() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 0, new King(PieceColor.WHITE)), new Game.PiecePlacement(0, 1, new Bishop(PieceColor.WHITE)), new Game.PiecePlacement(7, 7, new King(PieceColor.BLACK)), new Game.PiecePlacement(7, 5, new Bishop(PieceColor.BLACK))), PieceColor.WHITE);
            game._test_triggerUpdateGameState();
            assertNotEquals(Game.GameState.INSUFFICIENT_MATERIAL_DRAW, game.getGameState(), "Game should not be a draw by insufficient material.");
        }


        @Test
        @DisplayName("King and Pawn vs King is NOT a draw by insufficient material")
        void kingAndPawnVsKing() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 0, new King(PieceColor.WHITE)), new Game.PiecePlacement(1, 0, new Pawn(PieceColor.WHITE)), new Game.PiecePlacement(7, 7, new King(PieceColor.BLACK))), PieceColor.WHITE);
            game._test_triggerUpdateGameState();
            assertNotEquals(Game.GameState.INSUFFICIENT_MATERIAL_DRAW, game.getGameState(), "Game should not be a draw by insufficient material.");
        }

        @Test
        @DisplayName("King and Rook vs King is NOT a draw by insufficient material")
        void kingAndRookVsKing() {
            game.setupBoardForTest(List.of(new Game.PiecePlacement(0, 0, new King(PieceColor.WHITE)), new Game.PiecePlacement(1, 0, new Rook(PieceColor.WHITE)), new Game.PiecePlacement(7, 7, new King(PieceColor.BLACK))), PieceColor.WHITE);
            game._test_triggerUpdateGameState();
            assertNotEquals(Game.GameState.INSUFFICIENT_MATERIAL_DRAW, game.getGameState(), "Game should not be a draw by insufficient material.");
        }
    }

    @Nested
    @DisplayName("Threefold Repetition Tests")
    class ThreefoldRepetitionTests {
        private void makeSpecificMove(Game game, int r1, int c1, int r2, int c2) {
            Optional<Move> moveOpt = findMove(game.getAllLegalMovesForPlayer(game.getCurrentPlayer().getColor()), r1, c1, r2, c2);
            assertTrue(moveOpt.isPresent(), String.format("Move %s%d to %s%d not found for %s", (char) ('a' + c1), 8 - r1, (char) ('a' + c2), 8 - r2, game.getCurrentPlayer().getColor()));
            assertTrue(game.makeMove(moveOpt.get()));
        }

        private void makeSpecificPromotionMove(Game game, int r1, int c1, int r2, int c2, PieceType promotionType) {
            Optional<Move> moveOpt = findPromotionMove(game.getAllLegalMovesForPlayer(game.getCurrentPlayer().getColor()), r1, c1, r2, c2, promotionType);
            assertTrue(moveOpt.isPresent(), String.format("Promotion Move %s%d to %s%d=%s not found for %s", (char) ('a' + c1), 8 - r1, (char) ('a' + c2), 8 - r2, promotionType, game.getCurrentPlayer().getColor()));
            assertTrue(game.makeMove(moveOpt.get()));
        }


        @Test
        @DisplayName("Game is drawn after a position repeats three times with same side to move, castling rights, and en passant")
        void testThreefoldRepetitionDrawStrict() {
            game.initializeGame();

            makeSpecificMove(game, 7, 6, 5, 5); // Nf3
            long hashAfterW1 = game.getCurrentPositionHash();
            assertEquals(1, game.getPositionHistoryCount().getOrDefault(hashAfterW1, 0));

            makeSpecificMove(game, 0, 1, 2, 2); // Nc6
            long hashAfterB1 = game.getCurrentPositionHash();
            assertEquals(1, game.getPositionHistoryCount().getOrDefault(hashAfterB1, 0));

            makeSpecificMove(game, 5, 5, 7, 6); // Ng1
            long hashAfterW2 = game.getCurrentPositionHash();
            assertEquals(1, game.getPositionHistoryCount().getOrDefault(hashAfterW2, 0));


            makeSpecificMove(game, 2, 2, 0, 1); // Nb8
            long hashAfterB2 = game.getCurrentPositionHash();
            assertEquals(2, game.getPositionHistoryCount().getOrDefault(hashAfterB2, 0), "Initial position should have count 2.");


            makeSpecificMove(game, 7, 6, 5, 5); // Nf3
            assertEquals(2, game.getPositionHistoryCount().getOrDefault(game.getCurrentPositionHash(), 0), "Position after W:Nf3 (round 2) should have count 2.");
            assertEquals(hashAfterW1, game.getCurrentPositionHash());


            makeSpecificMove(game, 0, 1, 2, 2); // Nc6
            assertEquals(2, game.getPositionHistoryCount().getOrDefault(game.getCurrentPositionHash(), 0), "Position after B:Nc6 (round 2) should have count 2.");
            assertEquals(hashAfterB1, game.getCurrentPositionHash());
            assertEquals(Game.GameState.ACTIVE, game.getGameState());


            makeSpecificMove(game, 5, 5, 7, 6); // Ng1
            assertEquals(2, game.getPositionHistoryCount().getOrDefault(game.getCurrentPositionHash(), 0));
            assertEquals(hashAfterW2, game.getCurrentPositionHash());

            makeSpecificMove(game, 2, 2, 0, 1); // Nb8
            assertEquals(3, game.getPositionHistoryCount().getOrDefault(game.getCurrentPositionHash(), 0), "Initial position should now have count 3.");
            assertEquals(hashAfterB2, game.getCurrentPositionHash());
            assertEquals(Game.GameState.THREEFOLD_REPETITION_DRAW, game.getGameState());
        }

        @Test
        @DisplayName("Repetition with different castling rights does not count as same position")
        void testRepetitionDifferentCastlingRights() {
            game.initializeGame();

            makeSpecificMove(game, 6, 4, 4, 4); // e4
            makeSpecificMove(game, 1, 4, 3, 4); // e5
            long initialHash = game.getCurrentPositionHash();

            makeSpecificMove(game, 7, 4, 6, 4); // Ke2
            long hashPos2 = game.getCurrentPositionHash();
            assertNotEquals(initialHash, hashPos2);


            makeSpecificMove(game, 0, 4, 1, 4); // Ke7
            long hashPos3 = game.getCurrentPositionHash();

            makeSpecificMove(game, 6, 4, 7, 4); // Ke1
            long hashPos4 = game.getCurrentPositionHash();
            assertNotEquals(hashPos3, hashPos4);

            makeSpecificMove(game, 1, 4, 0, 4); // Ke8.
            long hashPos5 = game.getCurrentPositionHash();
            assertNotEquals(initialHash, hashPos5, "Hash should be different from initial due to lost castling rights.");
            assertEquals(1, game.getPositionHistoryCount().getOrDefault(hashPos5, 0));
            assertEquals(Game.GameState.ACTIVE, game.getGameState());
        }

        @Test
        @DisplayName("Repetition with different en passant target does not count as same position")
        void testRepetitionDifferentEnPassant() {
            game.initializeGame();
            makeSpecificMove(game, 6, 4, 4, 4); // 1. e4
            makeSpecificMove(game, 0, 1, 2, 2); // 1... Nc6
            makeSpecificMove(game, 4, 4, 3, 4); // 2. e5
            makeSpecificMove(game, 1, 3, 3, 3); // 3... d5
            long hashWithEnPassant = game.getCurrentPositionHash();

            makeSpecificMove(game, 7, 1, 5, 2); // 4. Nf3
            makeSpecificMove(game, 2, 2, 0, 1); // 4... Nb8
            makeSpecificMove(game, 5, 2, 7, 1); // 5. Nb1
            makeSpecificMove(game, 0, 1, 2, 2); // 5... Nc6
            long hashWithoutEnPassant = game.getCurrentPositionHash();
            assertNotEquals(hashWithEnPassant, hashWithoutEnPassant, "Hash should be different if en passant is null vs non-null.");
        }

        @Test
        @DisplayName("Undo move correctly restores position history count")
        void testUndoRestoresPositionHistory() {
            game.initializeGame();
            long initialHash = game.getCurrentPositionHash();
            assertEquals(1, game.getPositionHistoryCount().get(initialHash));

            makeSpecificMove(game, 6, 4, 4, 4); // 1. e4
            long hashAfterE4 = game.getCurrentPositionHash();
            assertEquals(1, game.getPositionHistoryCount().get(hashAfterE4));

            game.undoLastMove();

            assertEquals(initialHash, game.getCurrentPositionHash(), "Hash should revert to initial after undo.");
            assertEquals(1, game.getPositionHistoryCount().get(initialHash), "Count for initial hash should be 1 after undo.");
            assertNull(game.getPositionHistoryCount().get(hashAfterE4), "Count for hashAfterE4 should be null (or 0 if we don't remove keys).");
        }
    }
}
