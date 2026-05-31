/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example;

/**
 * Main simulation loop.
 * Emergency Medical Dispatch System - Main Simulation Loop
 *
 * Purporse:
 *  Entry point of the entire system. Builds the city graph,
 *  registers ambulance, and runs three test scenarios to
 *  demonstrate the full dispatch workflow.
 *
 * City Map:
 *         (4)       (2)
 *   Base 1 --- A --------- C
 *     |        |           |
 *    (7)      (3)         (5)
 *     |        |           |
 *   Base 2 --- B --------- D
 *                   (6)
 *
 * Edge weights = travel time in minutes
 */
public class Main {

    public static void main(String[] args) {
        testScenario1();
        testScenario2();
        testScenario3();
        testScenario4();
        testScenario5();
    }

        // ---------------------------------------------------
        // Build city graph
        // ---------------------------------------------------
        private static Graph buildCityGraph(){
            Graph city = new Graph();
            city.addEdge("Base1", "A", 4);
            city.addEdge("Base1", "Base2", 7);
            city.addEdge("A", "B", 3);
            city.addEdge("A", "C", 2);
            city.addEdge("B", "Base2", 5);
            city.addEdge("B", "D", 6);
            city.addEdge("C", "D", 5);
            return city;
        }

        // ---------------------------------------------------
        // Scenario from assignment
        // ---------------------------------------------------

        /*
        ===================================================
        Scenario 1
        System behavior point 1 & 2
            PQ automatically orders: Heart attack -> Fire -> Car accident
            System finds closest ambulance and computes shortest route (Djikstra)

            Both ambulances are FREE
            Call arrive in non-priority order.

            Call:
                Heart attack            (Severity 1) at A -> Priority Queue
                Minor car accident      (Severity 3) at B -> Regular Queue
                House fire              (Severity 2) at C -> Priority Queue

         Expected:
            PQ orders severity 1 before 2 before 3
            Nearest ambulance found via Djikstra for each call.
         ===================================================
         */
        private static void testScenario1(){
            printHeader("SCENARIO 1 - Priority ordering + Djikstra routing");
            System.out.println("    [System Behavior Point 1 & 2]");
            System.out.println("    Both ambulances FREE. Calls arrive in non-priority order.");
            System.out.println("    Expected order: Heart attack -> House fire -> Car accident\n");

            DispatchSystem system = new DispatchSystem(buildCityGraph());
            system.addAmbulance(new Ambulance("AMB-01","Base1"));
            system.addAmbulance(new Ambulance("AMB-02", "Base2"));

            system.receiveCall(new EmergencyCall("Heart Attack", "A", 1));
            system.receiveCall(new EmergencyCall("Minor Car Accident", "B", 3));
            system.receiveCall(new EmergencyCall("House Fire", "C", 2));

            system.printQueueStatus();
            printFooter();
        }

        /*
        ===================================================
        SCENARIO 2 — Both ambulances busy
        System Behavior Point 3:
            "If both ambulances are busy, the fire call waits
            in the Priority Queue and the car accident waits
            in the regular Queue."

        Both ambulances are BUSY from the start.
        Fire and car accident arrive — both must queue.

        Calls:
            House fire         (Severity 2) at C -> Priority Queue
            Minor car accident (Severity 3) at B -> Regular Queue

        Expected:
            Fire sits in Priority Queue.
            Car accident sits in Regular Queue.
            Neither dispatched until an ambulance is freed.
        ===================================================
         */
        private static void testScenario2() {
            printHeader("SCENARIO 2 — Both ambulances busy, calls wait in queues");
            System.out.println("  [System Behavior Point 3]");
            System.out.println("  Both ambulances BUSY. Fire -> PQ. Car accident -> Regular Queue.\n");

            DispatchSystem system = new DispatchSystem(buildCityGraph());

            Ambulance amb1 = new Ambulance("AMB-01", "Base1");
            Ambulance amb2 = new Ambulance("AMB-02", "Base2");
            amb1.dispatch(); // manually set both as BUSY
            amb2.dispatch();

            system.addAmbulance(amb1);
            system.addAmbulance(amb2);

            System.out.println("  AMB-01 -> BUSY");
            System.out.println("  AMB-02 -> BUSY\n");

            system.receiveCall(new EmergencyCall("House Fire",         "C", 2));
            system.receiveCall(new EmergencyCall("Minor Car Accident", "B", 3));

            // Show queues — fire should be in PQ, car accident in regular Queue
            System.out.println("\n  [Neither dispatched — both ambulances still busy]");
            system.printQueueStatus();
            printFooter();
        }

        /*
        ===================================================
        SCENARIO 3 — Ambulance freed, Priority Queue checked first
        System Behavior Point 4:
            "When an ambulance becomes free, the system checks
            the Priority Queue first. If empty, it takes the
            next call from the regular Queue."

            Both ambulances BUSY.
            Fire in Priority Queue, car accident in Regular Queue.
            AMB-01 is freed — system should take fire from PQ first.
            AMB-01 freed again — system takes car accident from Queue.

        Expected:
            AMB-01 freed -> dispatched to House fire (PQ first)
            AMB-01 freed -> dispatched to Car accident (Queue next)
        ===================================================
         */
        private static void testScenario3() {
            printHeader("SCENARIO 3 — Ambulance freed: Priority Queue checked first");
            System.out.println("  [System Behavior Point 4]");
            System.out.println("  Ambulance freed -> PQ checked first -> then Regular Queue.\n");

            DispatchSystem system = new DispatchSystem(buildCityGraph());

            Ambulance amb1 = new Ambulance("AMB-01", "Base1");
            Ambulance amb2 = new Ambulance("AMB-02", "Base2");
            amb1.dispatch(); // both start BUSY
            amb2.dispatch();

            system.addAmbulance(amb1);
            system.addAmbulance(amb2);

            // Queue up both calls while busy
            system.receiveCall(new EmergencyCall("House Fire",         "C", 2));
            system.receiveCall(new EmergencyCall("Minor Car Accident", "B", 3));

            // Free AMB-01 — system should pick from PQ first (House Fire)
            System.out.println("\n  [Freeing AMB-01 — system checks PQ first]");
            amb1.completeMission("Base1");
            system.processCalls();

            system.printQueueStatus();
            printFooter();
        }

        /*
        ===================================================
        SCENARIO 4 — Tie-breaking: same severity uses FIFO

        Two calls share the same severity.
        Earlier arrival should be dispatched first.

        Calls:
            House fire #1 (Severity 2) at C -> arrives first
            House fire #2 (Severity 2) at D -> arrives second
            Heart attack  (Severity 1) at A -> highest priority

        Expected:
            Heart attack -> House fire #1 -> House fire #2
        ===================================================
         */
        private static void testScenario4() {
            printHeader("SCENARIO 4 — Tie-breaking: same severity dispatched by FIFO");
            System.out.println("  Same severity -> earlier arrival wins.\n");

            DispatchSystem system = new DispatchSystem(buildCityGraph());
            system.addAmbulance(new Ambulance("AMB-01", "Base1"));
            system.addAmbulance(new Ambulance("AMB-02", "Base2"));

            system.receiveCall(new EmergencyCall("House Fire #1", "C", 2));
            system.receiveCall(new EmergencyCall("House Fire #2", "D", 2));
            system.receiveCall(new EmergencyCall("Heart Attack",  "A", 1));

            system.printQueueStatus();
            printFooter();
        }

        /*
        ===================================================
        SCENARIO 5 — All non-urgent calls (Regular Queue only)

        All calls are severity 3 — none go to Priority Queue.
        Regular Queue processes them in pure FIFO order.

        Calls:
            Broken leg     (Severity 3) at B -> arrives first
            Minor bruising (Severity 3) at D -> arrives second
            Twisted ankle  (Severity 3) at C -> arrives third

        Expected:
            Broken leg -> Minor bruising -> Twisted ankle
            (arrival order — pure FIFO)
        ===================================================
         */
        private static void testScenario5() {
            printHeader("SCENARIO 5 — All non-urgent calls (Regular Queue only, FIFO)");
            System.out.println("  All severity 3 -> Regular Queue only -> pure FIFO order.\n");

            DispatchSystem system = new DispatchSystem(buildCityGraph());
            system.addAmbulance(new Ambulance("AMB-01", "Base1"));
            system.addAmbulance(new Ambulance("AMB-02", "Base2"));

            system.receiveCall(new EmergencyCall("Broken Leg",     "B", 3));
            system.receiveCall(new EmergencyCall("Minor Bruising", "D", 3));
            system.receiveCall(new EmergencyCall("Twisted Ankle",  "C", 3));

            system.printQueueStatus();
            printFooter();
        }

        private static void printHeader(String title) {
            System.out.println(
                    "\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("  " + title);
            System.out.println(
                    "╚══════════════════════════════════════════════════════════════╝");
        }

        private static void printFooter() {
            System.out.println(
                    "\n────────────────────────────────────────────────────────────────");
        }
}