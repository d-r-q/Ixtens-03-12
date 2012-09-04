package ru.jdev.ixtens_03_12.protocol;

import java.io.Serializable;
import java.util.Arrays;

public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int trxId; // transaction id
    private final String serviceName;
    private final String methodName;
    private final Serializable[] args;

    public Request(int trxId, String serviceName, String methodName, Serializable[] args) {
        this.trxId = trxId;
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.args = args;
    }

    public int getTrxId() {
        return trxId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return String.format("Request (%d, %s, %s, %s)", trxId, serviceName, methodName, Arrays.toString(args));
    }
}
