package ru.jdev.ixtens_03_12.client;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncClient extends Client {

    private final ExecutorService callsExecutor = Executors.newCachedThreadPool();

    public AsyncClient(String host, int port) throws IOException, InterruptedException {
        super(host, port);
    }

    public Future<Serializable> callMethodAsync(final String serviceName, final String methodName, final Serializable... args) {
        checkArgs(args);
        return callsExecutor.submit(new Callable<Serializable>() {
            public Serializable call() throws Exception {
                try {
                    return callMethod(serviceName, methodName, args);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw e;
                } catch (Throwable throwable) {
                    throw new Exception(throwable);
                }
            }
        });
    }

}
