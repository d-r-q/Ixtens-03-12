package ru.jdev.ixtens_03_12.client.test;

import java.util.concurrent.*;

public class MemoryLeacksTest extends RmiTest {

    private TestClient secondClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        secondClient = new TestClient(hostName, PORT);
    }

    public void testMemoryLeacks() throws ExecutionException, InterruptedException, TimeoutException {
        ExecutorService es = Executors.newFixedThreadPool(4);
        final Future f1 = es.submit(new BigRequests(client, 1000));
        final Future f2 = es.submit(new BigRequests(client, 1000));
        final Future f3 = es.submit(new BigRequests(secondClient, 1000));
        final Future f4 = es.submit(new BigRequests(secondClient, 1000));
        f1.get(1, TimeUnit.HOURS);
        f2.get(1, TimeUnit.HOURS);
        f3.get(1, TimeUnit.HOURS);
        f4.get(1, TimeUnit.HOURS);
        es.shutdownNow();
    }

    @Override
    protected void tearDown() throws Exception {
        secondClient.dispose();
        super.tearDown();
    }

}
