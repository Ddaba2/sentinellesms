package com.sentinellesms.controller;

import com.sentinellesms.dto.phone.PhoneLookupResponse;
import com.sentinellesms.dto.phone.PhoneReputationResponse;
import com.sentinellesms.service.PhoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/phones")
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneService phoneService;

    @GetMapping("/lookup")
    public PhoneLookupResponse lookup(@RequestParam String number) {
        return phoneService.lookup(number);
    }

    @GetMapping
    public List<PhoneReputationResponse> list() {
        return phoneService.listAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        phoneService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
