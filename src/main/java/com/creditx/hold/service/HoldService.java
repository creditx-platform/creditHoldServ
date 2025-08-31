package com.creditx.hold.service;

import com.creditx.hold.dto.CreateHoldRequest;
import com.creditx.hold.dto.CreateHoldResponse;

public interface HoldService {
    CreateHoldResponse createHold(CreateHoldRequest request);
}
