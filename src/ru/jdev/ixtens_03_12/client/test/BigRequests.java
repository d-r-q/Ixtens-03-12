package ru.jdev.ixtens_03_12.client.test;

import java.util.concurrent.Callable;

public class BigRequests implements Callable<Object> {

    private final TestClient client;
    private final int commandsCount;

    public BigRequests(TestClient client, int commandsCount) {
        this.client = client;
        this.commandsCount = commandsCount;
    }

    public Object call() throws Exception {

        for (int i = 0; i < commandsCount; i++) {
            try {
                client.longTransmission();
            } catch (Throwable throwable) {
                throw new Exception(throwable);
            }
        }

        return null;
    }
}
