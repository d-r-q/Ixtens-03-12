package ru.jdev.ixtens_03_12.server;

import org.apache.log4j.Logger;
import ru.jdev.ixtens_03_12.common.RmiLogger;
import ru.jdev.ixtens_03_12.common.Transport;
import ru.jdev.ixtens_03_12.protocol.Request;
import ru.jdev.ixtens_03_12.protocol.Response;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class ClientHandler implements Runnable {

    private final RmiLogger logger = new RmiLogger(Logger.getLogger(ClientHandler.class));

    private final Transport transport;
    private final ExecutorService executorService;
    private final Invoker invoker;

    public ClientHandler(Transport transport, ExecutorService executorService, Invoker invoker) {
        this.transport = transport;
        this.executorService = executorService;
        this.invoker = invoker;
    }

    public void run() {
        try {
            while (!Thread.interrupted()) {
                try {
                    final Object command = transport.readCommand();
                    if (command instanceof Request) {
                        final Request request = (Request) command;
                        try {
                            executorService.submit(new InvokeTask(invoker, request, transport));
                        } catch (RejectedExecutionException e) {
                            if (executorService.isShutdown()) {
                                logger.info("Dispose request received");
                                break;
                            } else {
                                transport.writeObject(new Response(request.getTrxId(), null, new InvocationException("Server overloaded")));
                            }
                        }
                    } else {
                        // Не понятно кто подключился, продолжать нет смысла
                        logger.warn(String.format("Unexpected command received: %s", command));
                        break;
                    }
                } catch (EOFException e) {
                    logger.info("Remote endpoint closed");
                    break;
                } catch (IOException e) {
                    logger.warn("Connection lost");
                    break;
                } catch (InterruptedException e) {
                    logger.info("Dispose request received");
                    break;
                }
            }
        } finally {
            transport.close();
        }
    }
}
