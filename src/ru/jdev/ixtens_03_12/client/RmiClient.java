package ru.jdev.ixtens_03_12.client;

import org.apache.log4j.Logger;
import ru.jdev.ixtens_03_12.common.RmiLogger;
import ru.jdev.ixtens_03_12.server.InvocationException;

import java.io.IOException;
import java.net.InetAddress;

public class RmiClient {

    // да, знаю, что должно быть конфигурируемым, но лень
    private static final int PORT = 1986;
    private static final int THREADS_COUNT = 10;

    private static final RmiLogger logger = new RmiLogger(Logger.getLogger(RmiClient.class));

    public static void main(String[] args) {
        Client client = null;
        try {
            client = new Client(InetAddress.getLocalHost().getHostName(), PORT);
            final Thread[] threads = new Thread[THREADS_COUNT];
            for (int i = 0; i < THREADS_COUNT; i++) {
                threads[i] = new Thread(new Caller(client));
                threads[i].start();
            }

            for (int i = 0; i < THREADS_COUNT; i++) {
                threads[i].join();
            }

        } catch (IOException e) {
            logger.fatal("RMI Client creation failed", e);
        } catch (InterruptedException ignore) {
        } finally {
            if (client != null) {
                client.dispose();
            }
        }
    }

    private static class Caller implements Runnable {

        private final RmiLogger logger = new RmiLogger(Logger.getLogger(Caller.class));

        private final Client client;

        public Caller(Client client) {
            this.client = client;
        }

        public void run() {
            while (true) {
                try {
                    client.callMethod("Service1", "sleep", 1000L);
                    logger.info("Current Date is: %s", client.callMethod("Service1", "getCurrentDate"));
                } catch (InvocationException e) {
                    logger.fatal("Remote call failed", e);
                } catch (Throwable t) {
                    logger.fatal("Remote call failed", t);
                    break;
                }
            }
        }
    }


}
