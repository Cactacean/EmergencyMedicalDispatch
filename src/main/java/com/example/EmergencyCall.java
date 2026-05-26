package com.example;

/**
 * Represents an emergency call received by dispatch.
 */
public class EmergencyCall {

    private final String callerType;
    private final String location;
    private final int severity;

    /**
     * Severity:
     * 1 = highest priority
     * 2 = medium
     * 3 = lowest
     */
    public EmergencyCall(String callerType, String location, int severity) {
        this.callerType = callerType;
        this.location = location;
        this.severity = severity;
    }

    public String getCallerType() {
        return callerType;
    }

    public String getLocation() {
        return location;
    }

    public int getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "[" + callerType +
                " | Location=" + location +
                " | Severity=" + severity + "]";
    }
}