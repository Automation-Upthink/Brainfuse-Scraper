package com.upthink.Objects;

public class Triple<K, V, T> {
    private K key;
    private V value;
    private T bool;

    public Triple(K key, V value, T bool) {
        this.key = key;
        this.value = value;
        this.bool = bool;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public T getBool() {
        return bool;
    }
}
