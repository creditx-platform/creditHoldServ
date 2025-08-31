package com.creditx.hold.service;

import com.creditx.hold.dto.TransactionAuthorizedEvent;

public interface TransactionEventService {
    void processTransactionAuthorized(TransactionAuthorizedEvent event);
}
