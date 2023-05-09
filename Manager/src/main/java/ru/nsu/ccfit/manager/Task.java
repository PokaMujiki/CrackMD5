package ru.nsu.ccfit.manager;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private TaskStatus status;
    private final List<String> data = new ArrayList<>();

    public Task(TaskStatus status) {
        this.status = status;
    }

    public List<String> getData() {
        return data;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
