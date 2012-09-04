package ru.jdev.ixtens_03_12.client.test;

import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        InvokerTest.class,
        ProtocolTest.class,
        CloseServerTest.class,
        CloseClientTest.class,
        ClientsLimitTest.class,
        HighLoadTest.class,
        OverloadTest.class
})
public class RmiTestSuite extends TestSuite {

}
