package com.example;



/**
 * Represents an ambulance in the dispatch system.
 */
public class Ambulance {

    private final String id;
    private String currentLocation;
    private boolean available;

    public Ambulance(String id, String currentLocation) {
        this.id = id;
        this.currentLocation = currentLocation;
        this.available = true;
    }

    public String getId() {
        return id;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public boolean isAvailable() {
        return available;
    }

    public void dispatch() {
        available = false;
    }

    public void completeMission(String newLocation) {
        available = true;
        currentLocation = newLocation;
    }

    @Override
    public String toString() {
        return id + " @ " + currentLocation +
                (available ? " (AVAILABLE)" : " (BUSY)");
    }
}