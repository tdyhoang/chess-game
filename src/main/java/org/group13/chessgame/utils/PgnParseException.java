package org.group13.chessgame.utils;

public class PgnParseException extends RuntimeException {
    public PgnParseException(String message) {
        super(message);
    }

    public PgnParseException(String message, Throwable cause) {
        super(message, cause);
    }
}