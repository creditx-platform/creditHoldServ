package com.creditx.hold.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditx.hold.dto.CreateHoldRequest;
import com.creditx.hold.dto.CreateHoldResponse;
import com.creditx.hold.service.HoldService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/holds")
@RequiredArgsConstructor
public class HoldController {
    
    private final HoldService holdService;
    
    @PostMapping
    public ResponseEntity<CreateHoldResponse> createHold(@Validated @RequestBody CreateHoldRequest request) {
        var response = holdService.createHold(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
