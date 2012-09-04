package ru.jdev.ixtens_03_12.client.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CloseServerTest extends RmiTest {

    private TestClient secondClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        secondClient = new TestClient(hostName, PORT);
    }

    public void testCloseServer1() throws Exception {
        final ExecutorService es = Executors.newFixedThreadPool(5);
        es.submit(new ManyRequests(client, false));
        es.submit(new BigRequests(client, 10));
        es.submit(new ManyRequests(secondClient, false));
        es.submit(new BigRequests(secondClient, 10));

        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        final Future<?> stopFuture = es.submit(new Runnable() {
            public void run() {
                server.dispose();
            }
        });

        stopFuture.get(5, TimeUnit.SECONDS);

        es.shutdownNow();
        es.awaitTermination(5, TimeUnit.SECONDS);
    }

    public void testCloseServer2() throws Exception {
        final ExecutorService es = Executors.newFixedThreadPool(5);
        es.submit(new ManyRequests(client, false));
        es.submit(new ManyRequests(client, false));
        es.submit(new ManyRequests(client, false));
        es.submit(new ManyRequests(client, false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        final Future<?> stopFuture = es.submit(new Runnable() {
            public void run() {
                server.dispose();
            }
        });

        stopFuture.get(5, TimeUnit.SECONDS);

        es.shutdownNow();
        es.awaitTermination(5, TimeUnit.SECONDS);
    }

    public void testCloseServer3() throws Exception {
        final ExecutorService es = Executors.newFixedThreadPool(5);
        es.submit(new BigRequests(client, 10));
        es.submit(new BigRequests(client, 10));
        es.submit(new BigRequests(client, 10));
        es.submit(new BigRequests(client, 10));

        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        final Future<?> stopFuture = es.submit(new Runnable() {
            public void run() {
                server.dispose();
            }
        });

        stopFuture.get(5, TimeUnit.SECONDS);

        es.shutdownNow();
        es.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    protected void tearDown() throws Exception {
        secondClient.dispose();
        super.tearDown();
    }

}
