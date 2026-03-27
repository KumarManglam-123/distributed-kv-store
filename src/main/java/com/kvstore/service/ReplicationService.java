package com.kvstore.service;

import com.kvstore.model.KeyValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;

@Service
public class ReplicationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${node.peers:localhost:8002,localhost:8003}")
    private String peersConfig;

    public List<String> getPeers() {
        return Arrays.asList(peersConfig.split(","));
    }

    public void replicateToPeers(String key, String value) {
        KeyValue kv = new KeyValue(key, value);
        for (String peer : getPeers()) {
            new Thread(() -> {
                try {
                    String url = "http://" + peer + "/internal/replicate";
                    restTemplate.postForObject(url, kv, String.class);
                    System.out.println("Replicated to " + peer);
                } catch (Exception e) {
                    System.out.println("Failed to replicate to " + peer + ": " + e.getMessage());
                }
            }).start();
        }
    }
}