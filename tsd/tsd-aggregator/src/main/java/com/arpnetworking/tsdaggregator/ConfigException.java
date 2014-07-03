package com.arpnetworking.tsdaggregator;

/**
 * Exception throw when a configuration block cannot be parsed properly.
 *
 * @author barp
 */
@SuppressWarnings("WeakerAccess")
public class ConfigException extends Exception {
    public ConfigException() {
    }

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigException(Throwable cause) {
        super(cause);
    }

    public ConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
