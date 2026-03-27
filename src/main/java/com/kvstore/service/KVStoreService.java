package com.kvstore.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KVStoreService {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public void set(String key, String value) {
        store.put(key, value);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    public void delete(String key) {
        store.remove(key);
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(store);
    }

    public boolean containsKey(String key) {
        return store.containsKey(key);
    }
}