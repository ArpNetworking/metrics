package com.arpnetworking.tsdaggregator.aggserver;

import javax.annotation.Nullable;

/**
 * A concrete async result class for vertx.
 *
 * @author barp
 * @param <T> type to be returned as the async result
 */
public class ASResult<T> implements org.vertx.java.core.AsyncResult<T> {
    @Nullable
    private final T _result;
    @Nullable
    private final Throwable _throwable;

    ASResult(T result) {
        _result = result;
        _throwable = null;
    }

    ASResult(Throwable throwable) {
        _result = null;
        _throwable = throwable;
    }

    @Nullable
    @Override
    public T result() {
        return _result;
    }

    @Nullable
    @Override
    public Throwable cause() {
        return _throwable;
    }

    @Override
    public boolean succeeded() {
        return _throwable == null;
    }

    @Override
    public boolean failed() {
        return _throwable != null;
    }
}
