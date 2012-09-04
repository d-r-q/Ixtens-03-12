package ru.jdev.ixtens_03_12.client.test;

import ru.jdev.ixtens_03_12.client.Client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CloseClientTest extends RmiTest {

    private TestClient secondClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        secondClient = new TestClient(hostName, PORT);
    }

    public void testCloseClient1() throws Exception {
        final ExecutorService es = Executors.newFixedThreadPool(5);
        es.submit(new ManyRequests(client, false));
        es.submit(new BigRequests(client, 10));
        es.submit(new ManyRequests(secondClient, false));
        es.submit(new BigRequests(secondClient, 10));

        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        final Future<?> stopFuture1 = es.submit(new DisposeTask(client));
        final Future<?> stopFuture2 = es.submit(new DisposeTask(secondClient));

        stopFuture1.get(5, TimeUnit.SECONDS);
        stopFuture2.get(5, TimeUnit.SECONDS);

        es.shutdown();
        es.awaitTermination(2, TimeUnit.SECONDS);

        server.dispose();
    }

    public void testCloseClient2() throws Exception {
        final ExecutorService es = Executors.newFixedThreadPool(5);
        es.submit(new ManyRequests(client, false));
        es.submit(new ManyRequests(client, false));
        es.submit(new ManyRequests(client, false));
        es.submit(new ManyRequests(client, false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        final Future<?> stopFuture1 = es.submit(new DisposeTask(client));
        final Future<?> stopFuture2 = es.submit(new DisposeTask(secondClient));

        stopFuture1.get(5, TimeUnit.SECONDS);
        stopFuture2.get(5, TimeUnit.SECONDS);

        es.shutdown();
        es.awaitTermination(2, TimeUnit.SECONDS);

        server.dispose();
    }

    public void testCloseClient3() throws Exception {
        final ExecutorService es = Executors.newFixedThreadPool(5);
        es.submit(new BigRequests(client, 10));
        es.submit(new BigRequests(client, 10));
        es.submit(new BigRequests(client, 10));
        es.submit(new BigRequests(client, 10));

        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        final Future<?> stopFuture1 = es.submit(new DisposeTask(client));
        final Future<?> stopFuture2 = es.submit(new DisposeTask(secondClient));

        stopFuture1.get(5, TimeUnit.SECONDS);
        stopFuture2.get(5, TimeUnit.SECONDS);

        es.shutdown();
        es.awaitTermination(2, TimeUnit.SECONDS);

        server.dispose();
    }

    @Override
    protected void tearDown() throws Exception {
        secondClient.dispose();
        super.tearDown();
    }

    private class DisposeTask implements Runnable {

        private final Client client;

        private DisposeTask(Client client) {
            this.client = client;
        }

        public void run() {
            client.dispose();
        }
    }

}
