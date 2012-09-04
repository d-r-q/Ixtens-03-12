package ru.jdev.ixtens_03_12.client.test;

import ru.jdev.ixtens_03_12.client.AsyncClient;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.Future;

public class TestClient extends AsyncClient {
    public TestClient(String host, int port) throws IOException, InterruptedException {
        super(host, port);
    }

    public Future<Serializable> sleep(Long millis) {
        return callMethodAsync("Service1", "sleep", millis);
    }

    public Date getCurrentDate() throws Throwable {
        return (Date) callMethod("Service1", "getCurrentDate");
    }

    public void longTransmission() throws Throwable {
        callMethod("Service1", "longTransmission", new byte[1024 * 1024 * 15]);
    }

}
