package com.kvstore.service;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.*;

@Service
public class ConsistentHashService {

    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int replicas = 3;

    public void addNode(String node) {
        for (int i = 0; i < replicas; i++) {
            long hash = hash(node + "-" + i);
            ring.put(hash, node);
        }
    }

    public String getNode(String key) {
        if (ring.isEmpty()) return null;
        long hash = hash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        if (entry == null) entry = ring.firstEntry();
        return entry.getValue();
    }

    public List<String> getNodes(String key, int count) {
        if (ring.isEmpty()) return new ArrayList<>();
        long hash = hash(key);
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        NavigableMap<Long, String> tailMap = ring.tailMap(hash, true);
        Iterator<String> iter = tailMap.values().iterator();
        while (result.size() < count && iter.hasNext()) {
            String node = iter.next();
            if (!seen.contains(node)) {
                seen.add(node);
                result.add(node);
            }
        }
        if (result.size() < count) {
            for (String node : ring.values()) {
                if (result.size() >= count) break;
                if (!seen.contains(node)) {
                    seen.add(node);
                    result.add(node);
                }
            }
        }
        return result;
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            long h = 0;
            for (int i = 0; i < 4; i++) {
                h = (h << 8) | (digest[i] & 0xFF);
            }
            return h;
        } catch (Exception e) {
            return key.hashCode();
        }
    }
}