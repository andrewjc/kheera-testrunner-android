package com.kheera;

import com.kheera.annotations.Pending;

@Pending
public class PendingException extends RuntimeException {
    public PendingException() {
        this("TODO: implement me");
    }

    public PendingException(String message) {
        super(message);
    }
}
