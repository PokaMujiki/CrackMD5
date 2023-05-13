package ru.nsu.ccfit.manager.exception;

public class RabbitUnavailable extends Exception {
    public RabbitUnavailable() {}

    public RabbitUnavailable(String errorMessage) {
        super(errorMessage);
    }

    public RabbitUnavailable (Throwable cause) {
        super (cause);
    }

    public RabbitUnavailable (String message, Throwable cause) {
        super (message, cause);
    }
}
