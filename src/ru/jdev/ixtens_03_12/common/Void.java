package ru.jdev.ixtens_03_12.common;

import java.io.Serializable;

public class Void implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Void instance = new Void();

    private Void() {
    }

    @Override
    public String toString() {
        return "void";
    }
}
