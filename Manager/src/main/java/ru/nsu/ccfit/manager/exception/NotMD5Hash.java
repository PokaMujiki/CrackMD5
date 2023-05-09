package ru.nsu.ccfit.manager.exception;

public class NotMD5Hash extends Exception {
    public NotMD5Hash() {}

    public NotMD5Hash(String errorMessage) {
        super(errorMessage);
    }

    public NotMD5Hash (Throwable cause) {
        super (cause);
    }

    public NotMD5Hash (String message, Throwable cause) {
        super (message, cause);
    }
}
