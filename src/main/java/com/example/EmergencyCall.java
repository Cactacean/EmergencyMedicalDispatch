package com.example;

/**
 * Represents an emergency call received by dispatch.
 */
public class EmergencyCall {

    private final String callerType;
    private final String location;
    private final int severity;
    private final long timestamp; // edited missing part

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
        this.timestamp = System.currentTimeMillis(); // edited
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

    public long getTimestamp(){ return timestamp; } // edited

    // for output:
    // [Heart Attack | Location=A | Severity=1 (CRITICAL)]
    public String getSeverityLabel(){
        switch(severity){
            case 1:
                return "CRITICAL";
            case 2:
                return "SERIOUS";
            case 3:
                return "MINOR";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return "[" + callerType +
                " | Location=" + location +
                " | Severity=" + severity + "]" +
                " (" + getSeverityLabel() + ")"
                + "]";
    }
}