package com.kvstore.controller;

import com.kvstore.model.AuditLog;
import com.kvstore.model.KeyValue;
import com.kvstore.repository.AuditLogRepository;
import com.kvstore.service.ConsistentHashService;
import com.kvstore.service.KVStoreService;
import com.kvstore.service.ReplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class KVController {

    @Autowired
    private KVStoreService kvStoreService;

    @Autowired
    private ReplicationService replicationService;

    @Autowired
    private ConsistentHashService hashService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Value("${node.id:node1}")
    private String nodeId;

    @Value("${server.port:8001}")
    private String port;

    @Value("${node.peers:localhost:8002,localhost:8003}")
    private String peers;

    @PostConstruct
    public void init() {
        hashService.addNode("localhost:" + port);
        for (String peer : peers.split(",")) {
            hashService.addNode(peer);
        }
        System.out.println("Node " + nodeId + " started on port " + port);
    }

    @PostMapping("/set")
    public ResponseEntity<Map<String, String>> set(@RequestBody KeyValue kv) {
        kvStoreService.set(kv.getKey(), kv.getValue());
        replicationService.replicateToPeers(kv.getKey(), kv.getValue());
        auditLogRepository.save(new AuditLog("SET", kv.getKey(), kv.getValue(), nodeId));
        String owner = hashService.getNode(kv.getKey());
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "key", kv.getKey(),
            "node", nodeId,
            "owner", owner != null ? owner : "unknown"
        ));
    }

    @GetMapping("/get")
    public ResponseEntity<?> get(@RequestParam String key) {
        return kvStoreService.get(key)
            .map(value -> ResponseEntity.ok(
                Map.of("key", key, "value", value, "node", nodeId)))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> delete(@RequestParam String key) {
        kvStoreService.delete(key);
        auditLogRepository.save(new AuditLog("DELETE", key, "", nodeId));
        return ResponseEntity.ok(Map.of("status", "deleted", "key", key));
    }

    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> keys() {
        Map<String, String> all = kvStoreService.getAll();
        return ResponseEntity.ok(Map.of(
            "node", nodeId,
            "count", all.size(),
            "keys", all
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "nodeId", nodeId,
            "port", port
        ));
    }

    @PostMapping("/internal/replicate")
    public ResponseEntity<Map<String, String>> replicate(@RequestBody KeyValue kv) {
        kvStoreService.set(kv.getKey(), kv.getValue());
        return ResponseEntity.ok(Map.of("status", "replicated"));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLog>> audit() {
        return ResponseEntity.ok(auditLogRepository.findTop10ByOrderByTimestampDesc());
    }
}