package com.cleanifyai.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanifyai.api.dto.shared.PingResponse;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<PingResponse> ping() {
        return ResponseEntity.ok(new PingResponse("pong", "cleanifyai-api"));
    }
}

