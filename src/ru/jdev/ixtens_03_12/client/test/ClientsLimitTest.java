package ru.jdev.ixtens_03_12.client.test;

import junit.framework.TestCase;
import ru.jdev.ixtens_03_12.client.Client;
import ru.jdev.ixtens_03_12.server.RmiServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class ClientsLimitTest extends TestCase {

    private RmiServer server;

    @Override
    public void setUp() throws Exception {
        server = new RmiServer();
        server.startServer(1986);

        // тут хорошо бы сделать server.waitForStartup(); но, я решил, что это будет лишним
        Thread.sleep(300);
    }

    public void testClientsLimit() throws Exception {
        final Set<Client> clients = new HashSet<Client>();
        try {
            final String hostName = InetAddress.getLocalHost().getHostName();
            for (int i = 0; i < RmiServer.MAX_CLIENTS; i++) {
                clients.add(new Client(hostName, RmiTest.PORT));
            }

            try {
                new Client(hostName, RmiTest.PORT);
                fail();
            } catch (IOException expected) {
            }
        } finally {
            for (Client c : clients) {
                try {
                    c.dispose();
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        server.dispose();
    }
}
