package com.creditx.hold.service.impl;

import org.springframework.stereotype.Service;

import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.repository.HoldRepository;
import com.creditx.hold.service.TransactionEventService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionEventServiceImpl implements TransactionEventService {

    private final HoldRepository holdRepository;

    @Override
    @Transactional
    public void processTransactionAuthorized(TransactionAuthorizedEvent event) {
        // Find the hold by holdId and update status to CAPTURED
        Hold hold = holdRepository.findById(event.getHoldId())
                .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + event.getHoldId()));
        
        // Update hold status to CAPTURED
        hold.setStatus(HoldStatus.CAPTURED);
        holdRepository.save(hold);
    }
}
