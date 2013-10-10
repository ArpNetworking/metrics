package com.arpnetworking.tsdaggregator;

/**
 * An exception thrown when the log parser encounters a problem parsing a line.
 *
 * @author barp
 */
public class ParseException extends Exception {
    public ParseException() {
    }

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }

    public ParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
