package com.example.gateway.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.gateway.model.NapasRequest;
import com.example.gateway.model.NapasResponse;

@FeignClient(name = "napas-service", url = "http://localhost:8081/napas") // Cấu hình URL của NAPAS Service
public interface NapasClient {

    @PostMapping("/process") // Endpoint của NAPAS Service để xử lý request
    NapasResponse sendNapasRequest(@RequestBody NapasRequest napasRequest);
}