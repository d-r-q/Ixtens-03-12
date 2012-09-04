package ru.jdev.ixtens_03_12.client.test;

import ru.jdev.ixtens_03_12.server.InvocationException;

import java.io.Serializable;

public class ProtocolTest extends RmiTest {

    public void testNEService() throws Throwable {
        try {
            client.callMethod("Not Existing", "");
            fail();
        } catch (InvocationException expected) {
        }
    }

    public void testNEMethod() throws Throwable {
        try {
            client.callMethod("Service1", "Not Existing");
            fail();
        } catch (InvocationException expected) {
        }
    }

    public void testNoArgs() throws Throwable {
        try {
            client.callMethod("Service1", "sleep");
            fail();
        } catch (InvocationException expected) {
        }
    }

    public void testWrongArgs() throws Throwable {
        try {
            client.callMethod("Service1", "sleep", 10);
            fail();
        } catch (InvocationException expected) {
        }

        try {
            client.callMethod("Service1", "sleep", 10L, 20L);
            fail();
        } catch (InvocationException expected) {
        }
    }

    public void testVoid() throws Throwable {
        final Serializable res = client.callMethod("Service1", "voidMethod");
        assertTrue(res instanceof ru.jdev.ixtens_03_12.common.Void);
    }

    public void testArray() throws Throwable {
        final Serializable res = client.callMethod("Service1", "getArray", (Serializable)new Integer[]{1, 2, 3});
        assertTrue(res.getClass().isArray());
    }

    public void testNullReturn() throws Throwable {
        final Serializable res = client.callMethod("Service1", "getNull");
        assertNull(res);
    }

    public void testNullArgs() throws Throwable {
        try {
            client.callMethod("null", "null", null, null);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            client.callMethod("null", "null", (Serializable[])null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    public void testExceptions() throws Throwable {
        try {
            client.callMethod("Service1", "throwThrowable");
            fail();
        } catch (Throwable t) {
            assertTrue(t.getClass().equals(Throwable.class));
        }

        try {
            client.callMethod("Service1", "throwRuntimeException");
            fail();
        } catch (RuntimeException expected) {
        }

        try {
            client.callMethod("Service1", "throwException");
            fail();
        } catch (Exception expected) {
        }
    }

    public void testGetNotSerializable() throws Throwable {
        try {
            client.callMethod("Service1", "getNotSerializable");
            fail();
        } catch (InvocationException expected) {
        }
    }

}
