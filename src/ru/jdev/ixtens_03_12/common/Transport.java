package ru.jdev.ixtens_03_12.common;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

// При текущей реализации для передачи объекта требуется <size> * 4 памяти, т.к. он хрантся ввиде объектов и массивов
// и в клиенте и сервере. Знаю что не очень хорошо,  решил, что для тестового задания
// решение этой проблемы будет лишним

// Паттерн обращений к данному классу:
// * ровно один поток может читать
// * n потоков могут писать
// * n потоков могут закрывать
// Параллельные чтение и запись - к проблемам привести не может
// Запись синхронизована
// Синхронизация закрытия каналов и селекторов ими же и обеспечивается

// В случае возникновения ошибок протокола или сетевого ввода-вывода, данный объект берёт ответсвенность
// за освобождение ресурсов на себя
// Так же данный объект закрывает подключение в случае, если операции чтения/записи были интерраптнуты.
// Поэтому объекты данного класса нельзя шарить между потоками, один из которых может быть интерраптнут на
// операциях чтения/записи, а остальные могут попытаться продолжить работу (потоки с разным жизненым циклом).
// В кратце для потоков правило такое - либо все работаем, либо дружно заканчиваем работу
public class Transport {

    private static final int MAGIC_NUMBER = 1431655765;
    private static final int MAX_PACKAGE_LENGTH = 1024 * 1024 * 32;

    private static final int SOM_LENGTH = 8; // Start-Of-Message
    private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;

    private final RmiLogger logger = new RmiLogger(Logger.getLogger(Transport.class));

    private final SocketChannel socketChannel;
    private final Selector readSelector;
    private final Selector writeSelector;

    public Transport(SocketChannel socketChannel) throws IOException {
        // Выбор между стримами и каналами пал на каналы для того, чтобы транспорт был
        // чувствителен к интеррапту, что упрошает его использование в многопоточной среде
        this.socketChannel = socketChannel;
        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();

        socketChannel.configureBlocking(false);
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
    }

    public void writeObject(Serializable obj) throws IOException, InterruptedException {
        final byte[] cmd = serialize(obj);
        final String objStr = String.format("%s", obj);
        if (cmd.length > MAX_PACKAGE_LENGTH) {
            throw new IllegalArgumentException(String.format("To large command: %s", objStr));
        }
        final ByteBuffer buffer = ByteBuffer.allocate(SOM_LENGTH + cmd.length);
        buffer.order(BYTE_ORDER);
        buffer.putInt(MAGIC_NUMBER).putInt(cmd.length).put(cmd).flip();

        // синхронизуем потому что метод вызывается из нескольких потоков, а запись в неблокирующий сокетный канал
        // может за один раз записать не всё, и при параллельном обращении будет записан мусор
        synchronized (this) {
            while (buffer.remaining() > 0 && !Thread.interrupted()) {
                try {
                    writeSelector.select();
                    socketChannel.write(buffer);
                } catch (IOException e) {
                    close();
                    throw e;
                } catch (ClosedSelectorException e) {
                    close();
                    throw e;
                }
            }
        }

        if (buffer.remaining() != 0) {
            close();
            throw new InterruptedException();
        }

        logger.info("Object sended: %s", objStr);
    }

    // Я уверен, что вы и без меня знаете, что руками писать rmi java->java - это на раз 100500ый изобретать велосипед
    // В случае же, java->not-java использовать java-скую вообще безсмысленно
    // Поэтому делаю мифический задел на будущее и предполагаю, что сериализация будет переписана иным образом
    protected byte[] serialize(Serializable obj) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
        } finally {
            if (oos != null) {
                oos.close();
            }
        }

        return baos.toByteArray();
    }

    public Object readCommand() throws IOException, InterruptedException {
        // если отдавать этот класс куда-то "наружу", то тут надо сделать синхронизацию
        // но т.к. в данном случае пользователем являюсь только я и я могу сам себе гарантировать,
        // что этот метод не завётся параллельно, то я не стал делать синхронизацию
        try {
            return deserialize(readCommandBytes());
        } catch (ClosedSelectorException e) {
            close();
            throw e;
        } catch (IOException e) {
            close();
            throw e;
        } catch (InterruptedException e) {
            close();
            throw e;
        }
    }

    private byte[] readCommandBytes() throws IOException, InterruptedException {
        final ByteBuffer buffer = readData(SOM_LENGTH);
        final int magicNumber = buffer.getInt();
        final int length = buffer.getInt();
        if (magicNumber != MAGIC_NUMBER ||
                (length <= 0 || length > MAX_PACKAGE_LENGTH)) {
            // либо подключились не туда, либо протокол сбился - в обоих случаях продолжать работу смысла нет
            throw new IOException(String.format("Suspecting command received, magic number = %d, length = %d", magicNumber, length));
        }
        return readData(length).array();
    }

    // см. коммент к serialize
    protected Object deserialize(byte[] cmd) throws IOException {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(cmd));
            final Object obj = ois.readObject();
            logger.info("Object received: %s", obj);
            return obj;
        } catch (ClassNotFoundException e) {
            // подключились непонятно куда, продолжать смысла нет - свои с ошибкой программирования по логам разберуться,
            // а с незнакомцами лучше не разговаривать
            throw new IOException("Suspecting command received", e);
        } finally {
            if (ois != null) {
                ois.close();
            }
        }
    }

    private ByteBuffer readData(int len) throws IOException, InterruptedException {
        ByteBuffer buffer = ByteBuffer.allocate(len);
        buffer.order(BYTE_ORDER);
        while (buffer.remaining() > 0 && !Thread.interrupted()) {
            readSelector.select();
            final int readed = socketChannel.read(buffer);
            if (readed == -1) {
                throw new EOFException();
            }
        }

        if (buffer.remaining() != 0) {
            throw new InterruptedException();
        }

        buffer.flip();

        return buffer;
    }

    public void close() {
        try {
            socketChannel.close();
            readSelector.close();
            writeSelector.close();
        } catch (IOException ignore) {
        }
    }

}
