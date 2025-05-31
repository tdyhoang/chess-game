package org.group13.chessgame.utils;

import org.group13.chessgame.model.Game;
import org.group13.chessgame.model.Move;
import org.group13.chessgame.model.PieceColor;
import org.group13.chessgame.pgn.PgnHeaders;

import java.util.List;

public class PgnFormatter {

    public static String formatGame(PgnHeaders headers, List<Move> moveHistory, Game.GameState finalState) {
        StringBuilder pgn = new StringBuilder();

        // 1. Seven Tag Roster
        pgn.append(String.format("[Event \"%s\"]\n", headers.getEvent()));
        pgn.append(String.format("[Site \"%s\"]\n", headers.getSite()));
        pgn.append(String.format("[Date \"%s\"]\n", headers.getDate()));
        pgn.append(String.format("[Round \"%s\"]\n", headers.getRound()));
        pgn.append(String.format("[White \"%s\"]\n", headers.getWhite()));
        pgn.append(String.format("[Black \"%s\"]\n", headers.getBlack()));
        pgn.append(String.format("[Result \"%s\"]\n", headers.getResult()));
        pgn.append("\n");

        // 2. Movetext
        for (int i = 0; i < moveHistory.size(); i++) {
            Move move = moveHistory.get(i);
            if (move.getPieceMoved().getColor() == PieceColor.WHITE) {
                pgn.append((i / 2) + 1).append(". ");
            }
            pgn.append(move.getStandardAlgebraicNotation()).append(" ");

            if ((i + 1) % 16 == 0 && i < moveHistory.size() - 1) {
                pgn.append("\n");
            }
        }

        pgn.append(headers.getResult());

        return pgn.toString();
    }
}