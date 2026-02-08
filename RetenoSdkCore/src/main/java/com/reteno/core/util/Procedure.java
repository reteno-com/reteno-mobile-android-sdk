package com.reteno.core.util;

@FunctionalInterface
public interface Procedure<T> {
    void execute(T argument);
}