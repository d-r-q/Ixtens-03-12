package ru.jdev.ixtens_03_12.client.test;

import junit.framework.TestCase;
import org.apache.log4j.PropertyConfigurator;
import ru.jdev.ixtens_03_12.common.Constants;
import ru.jdev.ixtens_03_12.server.RmiServer;

import java.net.InetAddress;

public class RmiTest extends TestCase {

    static {
        PropertyConfigurator.configure(Constants.TEST_LOG4J_PROPERTIES);
    }

    public static final int PORT = 1986;

    protected String hostName;

    protected RmiServer server;
    protected TestClient client;

    @Override
    public void setUp() throws Exception {
        server = new RmiServer();
        server.startServer(1986);

        // тут хорошо бы сделать server.waitForStartup(); но, я решил, что это будет лишним
        Thread.sleep(300);
        hostName = InetAddress.getLocalHost().getHostName();
        client = new TestClient(hostName, PORT);
    }


    @Override
    protected void tearDown() throws Exception {
        client.dispose();
        server.dispose();
    }
}
