package org.group13.chessgame.pgn;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PgnHeaders implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String event;
    private String site;
    private String date;
    private String round;
    private String white;
    private String black;
    private String result;

    public PgnHeaders() {
        this.event = "?";
        this.site = "?";
        this.date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        this.round = "?";
        this.white = "White";
        this.black = "Black";
        this.result = "*";
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = (event != null && !event.isEmpty()) ? event : "?";
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = (site != null && !site.isEmpty()) ? site : "?";
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = (date != null && date.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) ? date : "????.??.??";
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = (round != null && !round.isEmpty()) ? round : "?";
    }

    public String getWhite() {
        return white;
    }

    public void setWhite(String white) {
        this.white = (white != null && !white.isEmpty()) ? white : "?";
    }

    public String getBlack() {
        return black;
    }

    public void setBlack(String black) {
        this.black = (black != null && !black.isEmpty()) ? black : "?";
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = (result != null && !result.isEmpty()) ? result : "*";
    }

    @Override
    public String toString() {
        return String.format("""
                [Event "%s"]
                [Site "%s"]
                [Date "%s"]
                [Round "%s"]
                [White "%s"]
                [Black "%s"]
                [Result "%s"]
                """, event, site, date, round, white, black, result);
    }
}