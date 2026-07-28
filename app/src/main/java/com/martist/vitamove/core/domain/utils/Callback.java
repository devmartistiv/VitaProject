package com.martist.vitamove.core.domain.utils;


@FunctionalInterface
public interface Callback<T> {


    void call(T param);
} 