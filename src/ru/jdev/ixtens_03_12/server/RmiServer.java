package ru.jdev.ixtens_03_12.server;

import org.apache.log4j.Logger;
import ru.jdev.ixtens_03_12.common.Constants;
import ru.jdev.ixtens_03_12.common.RmiLogger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.*;

public class RmiServer {

    // Все параметры хорошо бы сделать конфигурируемыми, но, т.к., код не промышленный, я не стал заморачиваться
    private static final String SERVICES_CONFIG = Constants.RES_DIR + "services.properties";

    // Мне не совсем понятно, зачем вы ввели требование создавать по потоку на клиента.
    // Может дело в том, что я работаю в условиях ограниченных ресурсов, но при мысли о создании более чем 40-50 потоков мне становится дурно
    // Это конечно от условий зависит (кол-во клиентов, трафик, требования ко времени ответа и т.д.), но я бы лучше сделал так:
    // * 1 поток на ассепт подключений
    // * 1 поток на мониторинг каналов
    // * пул потоков на вычитку данных
    // * пул потоков на исполнение и запись данных
    public static final int MAX_CLIENTS = 2;
    public static final int INVOKE_EXECUTORS_COUNT = MAX_CLIENTS * 3;

    private final RmiLogger logger = new RmiLogger(Logger.getLogger(RmiServer.class));
    // Фиксированный пул размером INVOKE_EXECUTORS_COUNT и очередью запросов INVOKE_EXECUTORS_COUNT * 2
    private final ExecutorService invokeRequestsExecutor = new ThreadPoolExecutor(INVOKE_EXECUTORS_COUNT, INVOKE_EXECUTORS_COUNT, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(INVOKE_EXECUTORS_COUNT * 2));
    // Кэширующий тред пул размером MAX_CLIENTS и без очереди запросов (запросы которые не могут быть обработы немедленно реджектаются)
    private final ExecutorService clientHandlersExecutor = new ThreadPoolExecutor(0, MAX_CLIENTS, 1, TimeUnit.MINUTES, new SynchronousQueue<Runnable>());
    private final Invoker invoker = new Invoker();

    private Thread acceptorThread;

    public void startServer(int port) throws IOException {
        final Properties services = new Properties();
        services.load(new FileInputStream(SERVICES_CONFIG));
        invoker.configure(services);
        final Acceptor acceptor = new Acceptor(port, clientHandlersExecutor, invoker, invokeRequestsExecutor);
        acceptorThread = new Thread(acceptor);
        acceptorThread.start();
    }

    public void dispose() {
        try {
            if (acceptorThread != null) {
                acceptorThread.interrupt();
                acceptorThread.join();
            }

            invokeRequestsExecutor.shutdownNow();
            invokeRequestsExecutor.awaitTermination(2, TimeUnit.MINUTES);

            clientHandlersExecutor.shutdownNow();
            clientHandlersExecutor.awaitTermination(2, TimeUnit.MINUTES);

        } catch (InterruptedException ignore) {
        }
    }

    public static void main(String[] args) {
        final int port;
        if (args.length != 1) {
            System.out.printf("Usage: java -cp <classpath> %s <port>\n", RmiServer.class.getCanonicalName());
            return;
        }
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.printf("Usage: java -cp <classpath> %s <port>\n", RmiServer.class.getCanonicalName());
            return;
        }

        final RmiServer server = new RmiServer();
        try {
            server.startServer(port);
        } catch (IOException e) {
            server.logger.fatal("Server start failed", e);
            server.dispose();
            return;
        }

        waitForExit();

        server.dispose();
    }

    private static void waitForExit() {
        System.out.println("Type \"exit\" to exit");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (!"exit".equals(reader.readLine())) ;
        } catch (IOException ignore) {
        }
    }

}
