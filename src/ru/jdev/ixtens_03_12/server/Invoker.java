package ru.jdev.ixtens_03_12.server;

import org.apache.log4j.Logger;
import ru.jdev.ixtens_03_12.common.RmiLogger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Invoker {

    private final RmiLogger logger = new RmiLogger(Logger.getLogger(Invoker.class));
    private final Map<String, Object> services = new HashMap<String, Object>();

    public void configure(Properties props) throws IOException {
        final Map<Class, Object> instances = new HashMap<Class, Object>();
        for (String serviceName : props.stringPropertyNames()) {
            try {
                final String className = props.getProperty(serviceName);
                final Class clazz = Class.forName(className);
                Object instance = instances.get(clazz);
                if (instance == null) {
                    instance = clazz.newInstance();
                    instances.put(clazz, instance);
                }
                services.put(serviceName, instance);
            } catch (ClassNotFoundException e) {
                logger.warn("Service \"%s\" creation failed", e, serviceName);
            } catch (InstantiationException e) {
                logger.warn("Service \"%s\" creation failed", e, serviceName);
            } catch (IllegalAccessException e) {
                logger.warn("Service \"%s\" creation failed", e, serviceName);
            }
        }
    }

    // Если отдавать этот класс "наружу", то надо либо синхронизировать invoke с configure, либо от пользователя требовать синхронизацию.
    // В данном же случае я сам себе могу гарантировать, что invoke и configure не будут зваться параллельно,
    // а параллельный вызов invoke к проблемам не приведёт
    public Object invoke(String serviceName, String methodName, Object... args) throws InvocationException, InvocationTargetException {
        final Object service = services.get(serviceName);
        if (service == null) {
            throw new InvocationException(String.format("Service \"%s\" not found", serviceName));
        }

        final Class[] argsClasses = getArgsClasses(args);
        try {
            // Вообще тут было бы хорошо реализовать более сложный поиск метода, который позволял бы
            // передавать значения с типами наследников типов параметров, но я решил, что в данном случае
            // это будет лишним (в текущей реализации, например, методу Map<Object, Object>.put можно передавать только
            // значения типа Object, и на тот же String либа ругнётся, что метод не найден)
            final Method method = service.getClass().getMethod(methodName, argsClasses);
            Object res = method.invoke(service, args);

            if (method.getReturnType().equals(void.class)) {
                res = ru.jdev.ixtens_03_12.common.Void.instance;
            }
            return res;
        } catch (NoSuchMethodException e) {
            throw new InvocationException(String.format("Method %s.%s(%s) not found", serviceName, methodName, toString(argsClasses)));
        } catch (IllegalAccessException e) {
            // не за чем сообщать клиенту о том, что метод в принципе существует, но у него нет доступа
            throw new InvocationException(String.format("Method %s.%s(%s) not found", serviceName, methodName, toString(argsClasses)));
        }
    }

    private Class[] getArgsClasses(Object... args) {
        final Class[] classes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            classes[i] = args[i].getClass();
        }
        return classes;
    }

    private String toString(Class[] argsClasses) {
        if (argsClasses.length == 0) {
            return "";
        }

        final StringBuilder res = new StringBuilder(argsClasses[0].getCanonicalName());
        for (int i = 1; i < argsClasses.length; i++) {
            res.append(',').append(argsClasses[i].getCanonicalName());
        }

        return res.toString();
    }

}
