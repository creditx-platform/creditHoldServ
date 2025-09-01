package com.creditx.hold.service;

import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.dto.TransactionPostedEvent;
import com.creditx.hold.dto.TransactionFailedEvent;

public interface TransactionEventService {
    void processTransactionAuthorized(TransactionAuthorizedEvent event);
    void processTransactionPosted(TransactionPostedEvent event);
    void processTransactionFailed(TransactionFailedEvent event);
}
