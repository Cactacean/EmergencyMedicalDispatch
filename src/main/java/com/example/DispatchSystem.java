/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example;
import java.util.*;

/**
 * Handles:
 * - Priority Queue (urgent calls)
 * - Regular Queue (non-urgent waiting calls)
 * - Ambulance assignment
 * - Workflow integration with Graph
 */
public class DispatchSystem {

    private final Graph cityGraph;

    // Severity 1 processed before 2 before 3
    private final PriorityQueue<EmergencyCall> priorityQueue =
            new PriorityQueue<>(Comparator.comparingInt(EmergencyCall::getSeverity));

    // FIFO queue for overflow/non-urgent calls
    private final Queue<EmergencyCall> regularQueue = new LinkedList<>();

    private final List<Ambulance> ambulances = new ArrayList<>();

    public DispatchSystem(Graph cityGraph) {
        this.cityGraph = cityGraph;
    }

    /**
     * Add ambulance into the system.
     */
    public void addAmbulance(Ambulance ambulance) {
        ambulances.add(ambulance);
    }

    /**
     * Receive incoming emergency call.
     */
    public void receiveCall(EmergencyCall call) {

        System.out.println("\nIncoming Call: " + call);

        // Severity 1 & 2 -> Priority Queue
        if (call.getSeverity() <= 2) {
            priorityQueue.offer(call);
            System.out.println("Added to PRIORITY queue.");
        }

        // Severity 3 -> Regular Queue
        else {
            regularQueue.offer(call);
            System.out.println("Added to REGULAR queue.");
        }

        processCalls();
    }

    /**
     * Main workflow logic.
     */
    public void processCalls() {

        while (hasAvailableAmbulance()) {

            EmergencyCall nextCall = null;

            // PRIORITY QUEUE FIRST
            if (!priorityQueue.isEmpty()) {
                nextCall = priorityQueue.poll();
                System.out.println("\nProcessing PRIORITY call: " + nextCall);
            }

            // THEN REGULAR FIFO QUEUE
            else if (!regularQueue.isEmpty()) {
                nextCall = regularQueue.poll();
                System.out.println("\nProcessing REGULAR call: " + nextCall);
            }

            // No calls remaining
            else {
                break;
            }

            assignAmbulance(nextCall);
        }
    }

    /**
     * Assign nearest ambulance using Graph module.
     */
    private void assignAmbulance(EmergencyCall call) {

        Map<String, String> availableAmbulances = new HashMap<>();

        for (Ambulance ambulance : ambulances) {
            if (ambulance.isAvailable()) {
                availableAmbulances.put(
                        ambulance.getId(),
                        ambulance.getCurrentLocation()
                );
            }
        }

        if (availableAmbulances.isEmpty()) {

            System.out.println("No ambulance available.");

            // Requeue depending on severity
            if (call.getSeverity() <= 2) {
                priorityQueue.offer(call);
            } else {
                regularQueue.offer(call);
            }

            return;
        }

        Object[] result = cityGraph.findNearestAmbulance(
                availableAmbulances,
                call.getLocation()
        );

        if (result == null) {
            System.out.println("No reachable ambulance.");
            return;
        }

        String ambulanceId = (String) result[0];
        Graph.PathResult path = (Graph.PathResult) result[1];

        Ambulance selected = findAmbulanceById(ambulanceId);

        selected.dispatch();

        System.out.println("Assigned Ambulance: " + ambulanceId);
        System.out.println(path);

        // Simulation:
        // ambulance instantly finishes mission
        selected.completeMission(call.getLocation());

        System.out.println(
                ambulanceId + " completed mission and is now AVAILABLE."
        );
    }

    /**
     * Check if at least one ambulance is free.
     */
    private boolean hasAvailableAmbulance() {
        for (Ambulance ambulance : ambulances) {
            if (ambulance.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find ambulance object by ID.
     */
    private Ambulance findAmbulanceById(String id) {
        for (Ambulance ambulance : ambulances) {
            if (ambulance.getId().equals(id)) {
                return ambulance;
            }
        }
        return null;
    }

    /**
     * Display queue states.
     */
    public void printQueueStatus() {

        System.out.println("\n=== QUEUE STATUS ===");

        System.out.println("Priority Queue:");
        for (EmergencyCall call : priorityQueue) {
            System.out.println(call);
        }

        System.out.println("\nRegular Queue:");
        for (EmergencyCall call : regularQueue) {
            System.out.println(call);
        }
    }
}