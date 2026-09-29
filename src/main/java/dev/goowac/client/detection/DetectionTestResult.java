package dev.goowac.client.detection;

public record DetectionTestResult(String check, String status, String detail, String module) {
    public boolean triggered() { return "TRIGGERED".equals(status); }
}
