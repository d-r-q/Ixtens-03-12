package ru.jdev.ixtens_03_12.client.test;

import java.util.Date;

public class TestService {

    public Date getCurrentDate() {
        return new Date();
    }

    public void voidMethod() {
    }

    public void sleep(Long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    public int longTransmission(byte[] data) {
        return data.length;
    }

    public Object getNull() {
        return null;
    }

    public void throwThrowable() throws Throwable {
        throw new Throwable();
    }

    public void throwException() throws Exception {
        throw new Exception();
    }

    public void throwRuntimeException() throws RuntimeException {
        throw new RuntimeException();
    }

    public NotSerializable getNotSerializable() {
        return new NotSerializable();
    }

    public Integer[] getArray(Integer[] arr) {
        return arr;
    }
    
}
