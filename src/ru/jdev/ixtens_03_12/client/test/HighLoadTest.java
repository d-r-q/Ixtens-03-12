package ru.jdev.ixtens_03_12.client.test;

import java.io.Serializable;
import java.util.concurrent.*;

public class HighLoadTest extends RmiTest {

    private TestClient secondClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        secondClient = new TestClient(hostName, PORT);
    }

    public void testManyRequests() throws Exception {
        final ExecutorService es = Executors.newFixedThreadPool(4);
        final Future f1 = es.submit(new ManyRequests(client, true));
        final Future f2 = es.submit(new ManyRequests(client, true));
        final Future f3 = es.submit(new ManyRequests(secondClient, true));
        final Future f4 = es.submit(new ManyRequests(secondClient, true));

        f1.get(2, TimeUnit.MINUTES);
        f2.get(2, TimeUnit.MINUTES);
        f3.get(2, TimeUnit.MINUTES);
        f4.get(2, TimeUnit.MINUTES);

        es.shutdownNow();
    }

    public void testBigAndManyRequests() throws ExecutionException, InterruptedException, TimeoutException {
        // Время ответа не проверяется, т.к. передача 20 мб может заблокировать канал на несколько секунд
        final ExecutorService es = Executors.newFixedThreadPool(4);
        final Future f1 = es.submit(new ManyRequests(client, false));
        final Future f2 = es.submit(new BigRequests(client, 10));
        final Future f3 = es.submit(new ManyRequests(secondClient, false));
        final Future f4 = es.submit(new BigRequests(secondClient, 10));

        f1.get(2, TimeUnit.MINUTES);
        f2.get(2, TimeUnit.MINUTES);
        f3.get(2, TimeUnit.MINUTES);
        f4.get(2, TimeUnit.MINUTES);

        es.shutdownNow();
    }

    public void testParallelRequests() throws ExecutionException, InterruptedException, TimeoutException {
        final ExecutorService es = Executors.newFixedThreadPool(4);
        final Future f1 = es.submit(new ParallelRequests(client));
        final Future f2 = es.submit(new ParallelRequests(client));
        final Future f3 = es.submit(new ParallelRequests(secondClient));
        final Future f4 = es.submit(new ParallelRequests(secondClient));

        f1.get(2, TimeUnit.MINUTES);
        f2.get(2, TimeUnit.MINUTES);
        f3.get(2, TimeUnit.MINUTES);
        f4.get(2, TimeUnit.MINUTES);

        es.shutdownNow();
    }

    @Override
    protected void tearDown() throws Exception {
        secondClient.dispose();
        super.tearDown();
    }

    private class ParallelRequests implements Callable<Object> {

        private final TestClient client;

        private ParallelRequests(TestClient client) {
            this.client = client;
        }

        public Object call() throws Exception {

            for (int i = 0; i < 10; i++) {
                Future<Serializable> f;
                try {
                    f = client.sleep(TimeUnit.SECONDS.toMillis(2));
                    client.getCurrentDate();
                } catch (Throwable throwable) {
                    throw new Exception(throwable);
                }
                assertFalse(f.isDone());
            }

            return null;
        }
    }

}
