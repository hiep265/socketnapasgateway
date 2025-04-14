package com.example.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gateway.model.GatewayRequest;
import com.example.gateway.model.NapasResponse;
import com.example.gateway.services.GatewayService;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @Autowired
    private GatewayService gatewayService;

    @PostMapping("/query")
    public ResponseEntity<NapasResponse> processQuery(@RequestBody GatewayRequest gatewayRequest) throws Exception {
        NapasResponse napasResponse = gatewayService.processNapasQuery(gatewayRequest);
        return ResponseEntity.ok(napasResponse);
    }
}
