package com.example.AIMSVER2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ServerController {

    @GetMapping("/checkServer")
    public ResponseEntity<Map<String, Object>> checkServer() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Server is running!");
        response.put("timestamp", LocalDateTime.now());
        response.put("application", "AIMSVER2");
        
        return ResponseEntity.ok(response);
    }
}
