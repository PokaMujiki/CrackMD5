package ru.nsu.ccfit.manager.models;

public class ManagerClientResponse {
    private String requestId;

    public ManagerClientResponse() {}

    public ManagerClientResponse(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
