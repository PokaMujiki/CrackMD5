package ru.nsu.ccfit.manager.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties({"finishedParts"})
public class Task {
    private TaskStatus status;
    private final List<String> data = new ArrayList<>();

    private final Set<Integer> finishedParts = new HashSet<>();

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

    public Set<Integer> getFinishedParts() {
        return finishedParts;
    }
}
