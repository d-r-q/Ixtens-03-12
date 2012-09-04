package ru.jdev.ixtens_03_12.client.test;

/**
 * User: jdev
 * Date: 04.03.12
 */
public class DublicatedServiceImpl {
    
    private static int instancesCount = 0;

    public DublicatedServiceImpl() {
        instancesCount++;
    }

    public static int getInstancesCount() {
        return instancesCount;
    }
}
