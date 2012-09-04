package ru.jdev.ixtens_03_12.client.test;

import ru.jdev.ixtens_03_12.server.InvocationException;
import ru.jdev.ixtens_03_12.server.RmiServer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class OverloadTest extends RmiTest {

    public void testOverLoad() throws Exception {

        final Set<Future> futures = new HashSet<Future>();
        // размер пула обработчиков + размер очереди запросов
        for (int i = 0; i < RmiServer.INVOKE_EXECUTORS_COUNT + RmiServer.INVOKE_EXECUTORS_COUNT * 2; i++) {
            futures.add(client.sleep(TimeUnit.SECONDS.toMillis(2)));
        }

        // Ждём, чтобы быть "уверенными" в том, что все запросы отправлены. Да, я знаю - малый хак
        Thread.sleep(500);
        final Future<Serializable> lastFuture = client.sleep(TimeUnit.SECONDS.toMillis(1));

        for (Future f : futures) {
            f.get();
        }

        try {
            lastFuture.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof InvocationException);
        }
    }
}
