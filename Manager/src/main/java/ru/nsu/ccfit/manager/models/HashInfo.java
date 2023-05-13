package ru.nsu.ccfit.manager.models;

public class HashInfo {
    private String hash;
    private int maxLength;

    public HashInfo() { }

    public  HashInfo(String hash, int maxLength) {
        this.hash = hash;
        this.maxLength = maxLength;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
