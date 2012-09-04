package ru.jdev.ixtens_03_12.client.test;

import junit.framework.Assert;

import java.util.concurrent.Callable;

public class ManyRequests implements Callable<Object> {

    private final TestClient client;
    private final boolean checkResponseTime;

    public ManyRequests(TestClient client, boolean checkResponseTime) {
        this.client = client;
        this.checkResponseTime = checkResponseTime;
    }

    public Object call() throws Exception {

        for (int i = 0; i < 25; i++) {
            long startTime = System.currentTimeMillis();
            try {
                client.getCurrentDate();
            } catch (Throwable throwable) {
                throw new Exception(throwable);
            }
            final long finishTime = System.currentTimeMillis();
            if (checkResponseTime) {
                Assert.assertTrue(String.format("getCurrentDate execution time: %d", finishTime - startTime), finishTime - startTime < 300);
            }
        }
        return null;
    }

}
