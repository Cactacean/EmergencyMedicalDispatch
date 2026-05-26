/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example;

/**
 * Main simulation loop.
 */
public class Main {

    public static void main(String[] args) {

        // ---------------------------------------------------
        // Build city graph
        // ---------------------------------------------------
        Graph city = new Graph();

        city.addEdge("Base1", "A", 4);
        city.addEdge("Base1", "Base2", 7);
        city.addEdge("A", "B", 3);
        city.addEdge("A", "C", 2);
        city.addEdge("B", "Base2", 5);
        city.addEdge("B", "D", 6);
        city.addEdge("C", "D", 5);

        // ---------------------------------------------------
        // Create dispatch system
        // ---------------------------------------------------
        DispatchSystem system = new DispatchSystem(city);

        // Add ambulances
        system.addAmbulance(new Ambulance("AMB-01", "Base1"));
        system.addAmbulance(new Ambulance("AMB-02", "Base2"));

        // ---------------------------------------------------
        // Scenario from assignment
        // ---------------------------------------------------

        // 1. Heart attack (highest)
        system.receiveCall(
                new EmergencyCall(
                        "Heart Attack",
                        "A",
                        1
                )
        );

        // 2. Minor car accident (lowest)
        system.receiveCall(
                new EmergencyCall(
                        "Minor Car Accident",
                        "B",
                        3
                )
        );

        // 3. House fire (medium)
        system.receiveCall(
                new EmergencyCall(
                        "House Fire",
                        "C",
                        2
                )
        );

        // ---------------------------------------------------
        // Final queue state
        // ---------------------------------------------------
        system.printQueueStatus();
    }
}