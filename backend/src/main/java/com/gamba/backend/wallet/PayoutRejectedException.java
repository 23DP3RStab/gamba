package com.gamba.backend.wallet;

import org.springframework.http.HttpStatus;

/** A definite rejection of THIS submission. Earlier uncertain submissions still need reconciliation. */
public class PayoutRejectedException extends PaymentException {
    public PayoutRejectedException(String code, String message) {
        super(HttpStatus.BAD_GATEWAY, code, message);
    }
}
