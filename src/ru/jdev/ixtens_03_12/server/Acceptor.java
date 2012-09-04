package ru.jdev.ixtens_03_12.server;

import org.apache.log4j.Logger;
import ru.jdev.ixtens_03_12.common.RmiLogger;
import ru.jdev.ixtens_03_12.common.Transport;
import ru.jdev.ixtens_03_12.protocol.ConnectionAccepted;
import ru.jdev.ixtens_03_12.protocol.ConnectionRejected;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class Acceptor extends Thread {

    private final RmiLogger logger = new RmiLogger(Logger.getLogger(Acceptor.class));

    private final ServerSocketChannel serverSocketChannel;

    private final ExecutorService handlersExecutor;
    private final ExecutorService requestsExecutor;
    private final Invoker invoker;

    public Acceptor(int port, ExecutorService handlersExecutor, Invoker invoker, ExecutorService requestsExecutor) throws IOException {
        this.invoker = invoker;
        this.requestsExecutor = requestsExecutor;

        serverSocketChannel = ServerSocketChannel.open();

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), port);
        serverSocketChannel.socket().bind(address);
        logger.info("Server started at %s", address.toString());

        this.handlersExecutor = handlersExecutor;
    }

    @Override
    public void run() {
        try {
            while (!interrupted()) {
                SocketChannel socketChannel;
                try {
                    socketChannel = serverSocketChannel.accept();
                    logger.info("Channel accepted: %s", socketChannel.socket().getRemoteSocketAddress());
                } catch (ClosedByInterruptException e) {
                    logger.info("Dispose request received");
                    break;
                } catch (IOException e) {
                    logger.fatal("Accept failed");
                    break;
                }

                try {
                    final Transport transport = new Transport(socketChannel);
                    try {
                        handlersExecutor.submit(new ClientHandler(transport, requestsExecutor, invoker));
                        transport.writeObject(new ConnectionAccepted());
                    } catch (RejectedExecutionException e) {
                        if (handlersExecutor.isShutdown()) {
                            logger.info("Dispose request received");
                            transport.close();
                            break;
                        } else {
                            logger.warn("Clients limit exceeded");
                            transport.writeObject(new ConnectionRejected());
                            transport.close();
                        }
                    }
                } catch (IOException e) { // транспорт сам позаботится о том, чтобы закрыться
                    logger.warn("Transport initialization failed");
                } catch (InterruptedException e) {
                    logger.info("Dispose request received");
                    break;
                }
            }
        } finally {
            try {
                serverSocketChannel.close();
            } catch (IOException ignore) {
            }
        }
    }

}
