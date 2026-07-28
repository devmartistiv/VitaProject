package com.martist.vitamove.core.domain.utils;


public interface SupabaseCallback<T> {


    void onSuccess(T result);


    void onFailure(Exception e);
} 