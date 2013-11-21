package com.arpnetworking.tsdaggregator.aggserver;

/**
 * A concrete async result class for vertx.
 *
 * @author barp
 */
public class ASResult<T> implements org.vertx.java.core.AsyncResult<T> {
    private final T _result;
    private final Throwable _throwable;

    ASResult(T result) {
        _result = result;
        _throwable = null;
    }

    ASResult(Throwable throwable) {
        _result = null;
        _throwable = throwable;
    }

    @Override
    public T result() {
        return _result;
    }

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
