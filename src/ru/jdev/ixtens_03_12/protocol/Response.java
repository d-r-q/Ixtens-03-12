package ru.jdev.ixtens_03_12.protocol;

import java.io.Serializable;

public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int trxId; // transaction id
    private final Serializable res;
    private final Throwable exception;

    public Response(int trxId, Serializable res, Throwable exception) {
        this.trxId = trxId;
        this.res = res;
        this.exception = exception;
    }

    public int getTrxId() {
        return trxId;
    }

    public Serializable getRes() {
        return res;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public String toString() {
        return String.format("Response(%d, %s, %s)", trxId, res, exception);
    }

}
