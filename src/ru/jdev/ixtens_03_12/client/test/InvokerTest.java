package ru.jdev.ixtens_03_12.client.test;

import junit.framework.TestCase;
import ru.jdev.ixtens_03_12.server.Invoker;

import java.util.Properties;

public class InvokerTest extends TestCase {

    public void testTestInvoker() throws Exception {
        final Properties props = new Properties();
        props.put("service1", DublicatedServiceImpl.class.getCanonicalName());
        props.put("service2", DublicatedServiceImpl.class.getCanonicalName());
        final Invoker invoker = new Invoker();
        invoker.configure(props);

        assertEquals(1, DublicatedServiceImpl.getInstancesCount());
    }
}
