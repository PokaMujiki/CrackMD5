package ru.nsu.ccfit.manager.exception;

public class NoSuchTask extends Exception {
    public NoSuchTask() {}

    public NoSuchTask(String errorMessage) {
        super(errorMessage);
    }

    public NoSuchTask (Throwable cause) {
        super (cause);
    }

    public NoSuchTask (String message, Throwable cause) {
        super (message, cause);
    }
}
