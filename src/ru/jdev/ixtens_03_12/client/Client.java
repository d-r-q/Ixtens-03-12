package ru.jdev.ixtens_03_12.client;

import org.apache.log4j.Logger;
import ru.jdev.ixtens_03_12.common.RmiLogger;
import ru.jdev.ixtens_03_12.common.Transport;
import ru.jdev.ixtens_03_12.protocol.ConnectionAccepted;
import ru.jdev.ixtens_03_12.protocol.ConnectionRejected;
import ru.jdev.ixtens_03_12.protocol.Request;
import ru.jdev.ixtens_03_12.protocol.Response;
import ru.jdev.ixtens_03_12.server.InvocationException;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {

    private final Hashtable<Integer, Response> responses = new Hashtable<Integer, Response>();
    private final AtomicInteger nextRequestId = new AtomicInteger(0);
    private final RmiLogger logger = new RmiLogger(Logger.getLogger(Client.class));

    private final Transport transport;

    private boolean hasReader;

    public Client(String host, int port) throws IOException, InterruptedException {
        final SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(host, port));

        transport = new Transport(channel);
        final Object serverAnswer = transport.readCommand();
        if (serverAnswer instanceof ConnectionAccepted) {
            logger.trace("Connection established");
        } else if (serverAnswer instanceof ConnectionRejected) {
            transport.close();
            throw new IOException("Connection rejected");
        } else {
            // подключились непонятно куда, продолжать смысла нет
            transport.close();
            throw new IOException(String.format("Unexpected server answer: %s", serverAnswer));
        }
    }

    public Serializable callMethod(String serviceName, String methodName, Serializable... args) throws Throwable {
        checkArgs(args);

        final int trxId = nextRequestId.getAndIncrement();
        transport.writeObject(new Request(trxId, serviceName, methodName, args));

        final Response response = getResponse(trxId);

        final Throwable ex = response.getException();
        if (ex != null) {
            throw ex;
        }

        return response.getRes();
    }

    protected void checkArgs(Serializable... args) {
        if (args == null) {
            throw new NullPointerException("args cannot be null");
        }
        // В текущей реализации Invoker'а у всех аргументов должны быть явно прописаны типы, а у null типа нет
        for (Serializable arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException(String.format("Null arguments is not supported, args: %s", Arrays.toString(args)));
            }
        }
    }

    // Это, на мой взгляд, достаточно интересный метод. В принципе, задачу ожидания ответа можно решить проще:
    // завести отдельный поток на вычитку, который складывает ответы в мапу, а потоки вызова просто ждут на мапе нужного
    // ответа. Но в этом случае, во-первых, появляется "лишний" поток, что на мой взгляд всегда плохо и, во-вторых,
    // закрытие клиента будет проходить всегда через исключение, что, опять таки, на мой взгляд тоже всегда плохо

    // Я же здесь предлагаю варинт, когда чтение происходит только тогда, когда оно кому-то необходимо, что позволяет
    // избавится от обеих "нехорошестей" описанных выше. Суть следующая:
    // 1) Первым делом проверяем, не выполнена ли уже кем-либо наша работа, и если выполнена выходим
    // 2) Если другой поток уже читает, ждём пока он дочитает
    // 3) Если другой потока вычитал наши данные - выходим, если же он вычитал чужие, а нам дали время - идём сами работать
    //    на благо себя или общества
    // 4) Вычитав что-либо идём на шаг 1

    // В случае "долгий вызов, быстрый вызов" последовательность будет примерно следующая:
    // 1) отправляется долгий вызов
    // 2) поток долгого вызова видит, что транспорт свободен и начинает читать
    // 3) отправляется быстрый вызов
    // 4) поток бдыстрого вызова видит, что транспорт занят и начинает ждать
    // 5) поток долгого вызова вычитывает ответ быстрого вызова, складывает его в мапу, дёргкает второй поток и заходит
    //    на второй круг
    // 6) поток быстрого вызова, очнувшись, видит что данные есть и выходит
    // 7) поток долгого вызова вычитывает свой ответ, складывает его в мапу, заходит на второй круг, находит свой ответ и выходит

    // Наконец, очевидно, что предложенный вариант применим, только в случе, когда сервер не может отправлять сообщения
    // по собственной инициативе, но это как раз и есть наш случай
    private Response getResponse(int trxId) throws IOException, InterruptedException, InvocationException {
        while (true) {
            // responses - это Hashtable, так что синхронизировать доступ не требуется
            Response response = responses.remove(trxId);
            if (response != null) {
                return response;
            }
            synchronized (this) {
                // Пока кто-то читает за нас, ждём когда прочитают наш ответ
                while (hasReader && responses.get(trxId) == null) {
                    this.wait();
                }
                response = responses.remove(trxId);
                if (response != null) {
                    return response;
                }

                // За нас наш ответ не вычитали, так что начинаем читать сами
                hasReader = true;
            }
            try {
                final Object cmd = transport.readCommand();
                if (cmd instanceof Response) {
                    response = (Response) cmd;
                    responses.put(response.getTrxId(), response);
                    logger.trace(String.format("Received response for %d", response.getTrxId()));
                } else {
                    // подключились непонятно куда, продолжать смысла нет
                    transport.close();
                    throw new IOException(String.format("Unexpected response from server: %s", cmd));
                }
            } finally {
                synchronized (this) {
                    this.hasReader = false;
                    this.notifyAll();
                }
            }
        }
    }

    public void dispose() {
        transport.close();
    }

}
