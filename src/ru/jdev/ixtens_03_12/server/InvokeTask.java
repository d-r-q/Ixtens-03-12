package ru.jdev.ixtens_03_12.server;

import org.apache.log4j.Logger;
import ru.jdev.ixtens_03_12.common.RmiLogger;
import ru.jdev.ixtens_03_12.common.Transport;
import ru.jdev.ixtens_03_12.protocol.Request;
import ru.jdev.ixtens_03_12.protocol.Response;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

public class InvokeTask implements Runnable {

    private final RmiLogger logger = new RmiLogger(Logger.getLogger(InvokeTask.class));

    private final Invoker invoker;
    private final Request request;
    private final Transport transport;

    public InvokeTask(Invoker invoker, Request request, Transport transport) {
        this.invoker = invoker;
        this.request = request;
        this.transport = transport;
    }

    public void run() {
        Object res = null;
        Throwable ex = null;
        try {
            res = invoker.invoke(request.getServiceName(), request.getMethodName(), request.getArgs());
            if (res != null && !(res instanceof Serializable)) {
                res = null;
                ex = new InvocationException("Service returns not serializable object");
            }
        } catch (InvocationException e) {
            logger.warn("Invoke failed, request: %s", e, request);
            ex = e;
        } catch (InvocationTargetException e) {
            logger.warn("Invoke failed, request: %s", e, request);
            ex = e.getTargetException();
        }

        final Response resp = new Response(request.getTrxId(), (Serializable) res, ex);
        try {
            transport.writeObject(resp);
        } catch (IOException e) { // транспорт сам позаботится о том, чтобы закрыться
            logger.warn("Response writing failed");
        } catch (InterruptedException e) {
            logger.info("Dispose request received");
        }
    }
}
