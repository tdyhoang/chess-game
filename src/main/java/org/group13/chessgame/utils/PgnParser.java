package org.group13.chessgame.utils;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.github.bhlangonijr.chesslib.pgn.PgnIterator;
import org.group13.chessgame.model.Board;
import org.group13.chessgame.model.Game;
import org.group13.chessgame.model.PieceType;
import org.group13.chessgame.model.Square;
import org.group13.chessgame.pgn.PgnHeaders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PgnParser {

    public static Game parsePgn(String pgnString) throws Exception {
        Path tempFile = null;
        Game loadedGameModel;
        List<com.github.bhlangonijr.chesslib.game.Game> gamesFromPgn = new ArrayList<>();
        try {
            tempFile = Files.createTempFile("pgnInput", ".pgn");
            java.nio.file.Files.writeString(tempFile, pgnString);

            PgnIterator gamesIterator = new PgnIterator(tempFile.toString());

            for (com.github.bhlangonijr.chesslib.game.Game chesslibGameCandidate : gamesIterator) {
                gamesFromPgn.add(chesslibGameCandidate);
            }

            if (gamesFromPgn.isEmpty()) {
                throw new PgnParseException("No games found in PGN content via PgnIterator.");
            }

            com.github.bhlangonijr.chesslib.game.Game chesslibGame = gamesFromPgn.getFirst();

            chesslibGame.loadMoveText();
            MoveList chesslibHalfMoves = chesslibGame.getHalfMoves();

            loadedGameModel = new Game();
            loadedGameModel.initializeGame();

            PgnHeaders headers = getPgnHeaders(chesslibGame);
            loadedGameModel.setPgnHeaders(headers);

            for (com.github.bhlangonijr.chesslib.move.Move libMove : chesslibHalfMoves) {
                com.github.bhlangonijr.chesslib.Square libFrom = libMove.getFrom();
                com.github.bhlangonijr.chesslib.Square libTo = libMove.getTo();
                com.github.bhlangonijr.chesslib.Piece libPromotion = libMove.getPromotion();

                Square fromSquareModel = convertSquare(libFrom, loadedGameModel.getBoard());
                Square toSquareModel = convertSquare(libTo, loadedGameModel.getBoard());
                PieceType promotionTypeModel = (libPromotion != Piece.NONE && libPromotion != null) ? convertPieceType(libPromotion.getPieceType()) : null;

                boolean moveApplied = loadedGameModel.makeMoveFromSquares(fromSquareModel, toSquareModel, promotionTypeModel);

                if (!moveApplied) {
                    throw new PgnParseException("Failed to apply move " + libFrom + "-" + libTo + (promotionTypeModel != null ? "=" + promotionTypeModel : "") + " to game model. Move number approx: " + (loadedGameModel.getMoveHistory().size() / 2 + 1) + ". FEN: " + loadedGameModel.getFen());
                }
            }
        } finally {
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (java.io.IOException e) {
                    System.err.println("Warning: Could not delete temporary PGN file: " + tempFile + " - " + e.getMessage());
                }
            }
        }
        return loadedGameModel;
    }

    private static PgnHeaders getPgnHeaders(com.github.bhlangonijr.chesslib.game.Game chesslibGame) {
        PgnHeaders headers = new PgnHeaders();
        if (chesslibGame.getRound() != null && chesslibGame.getRound().getEvent() != null) {
            headers.setEvent(chesslibGame.getRound().getEvent().getName());
        }
        headers.setSite(chesslibGame.getRound().getEvent().getSite());
        headers.setDate(chesslibGame.getDate());
        if (chesslibGame.getRound() != null) {
            headers.setRound(String.valueOf(chesslibGame.getRound().getNumber()));
        }
        headers.setWhite(chesslibGame.getWhitePlayer().getName());
        headers.setBlack(chesslibGame.getBlackPlayer().getName());
        headers.setResult(chesslibGame.getResult() != null ? chesslibGame.getResult().getDescription() : "*");
        return headers;
    }

    private static Square convertSquare(com.github.bhlangonijr.chesslib.Square libSquare, Board board) {
        if (libSquare == null || libSquare == com.github.bhlangonijr.chesslib.Square.NONE) {
            System.err.println("PgnParser: Encountered null or NONE chesslib.Square during conversion.");
            return null;
        }

        int col = libSquare.getFile().ordinal();
        int modelRow = (Board.SIZE - 1) - libSquare.getRank().ordinal();

        return board.getSquare(modelRow, col);
    }

    private static PieceType convertPieceType(com.github.bhlangonijr.chesslib.PieceType libPieceType) {

        if (libPieceType == null) {
            System.err.println("PgnParser: Encountered null chesslib.PieceType during conversion.");
            return null;
        }

        return switch (libPieceType) {
            case PAWN -> PieceType.PAWN;
            case KNIGHT -> PieceType.KNIGHT;
            case BISHOP -> PieceType.BISHOP;
            case ROOK -> PieceType.ROOK;
            case QUEEN -> PieceType.QUEEN;
            case KING -> PieceType.KING;
            default -> throw new IllegalArgumentException("Unknown chesslib.PieceType encountered: " + libPieceType);
        };
    }
}