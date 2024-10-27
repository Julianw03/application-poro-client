package com.iambadatplaying.ressourceServer;

import java.util.*;

public class LRUCacheMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCacheMap(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
