# Emergency Medical Dispatch System

A Java-based Emergency Medical Dispatch System that simulates how emergency calls are received, prioritised, queued, and assigned to the nearest available ambulance.

The system combines priority queue logic, regular FIFO queue handling, graph-based routing, and Dijkstra’s shortest path algorithm to model a simplified ambulance dispatch workflow.

---

## Project Overview

This project demonstrates a simplified emergency medical dispatch process.

When an emergency call enters the system, the call is classified by severity. Critical and serious cases are placed into a Priority Queue, while minor cases are placed into a Regular Queue. The system then checks for available ambulances, calculates the shortest route from each available ambulance to the emergency location using Dijkstra’s algorithm, and dispatches the ambulance with the shortest estimated travel time.

The project includes both:

- A console-based simulation through `Main.java`
- A graphical Swing dashboard through `IODisplay.java`

---

## Main Features

- Receive emergency calls with different severity levels
- Classify calls into Priority Queue or Regular Queue
- Process Severity 1 and Severity 2 calls before Severity 3 calls
- Use FIFO ordering for regular non-urgent calls
- Assign the nearest available ambulance
- Represent city locations and roads using a weighted graph
- Calculate shortest routes using Dijkstra’s algorithm
- Track ambulance status as available or busy
- Simulate multiple dispatch scenarios
- Provide a GUI dashboard with tables, logs, city map, and route animation

---

## Severity Levels

| Severity | Label | Meaning | Queue Type |
|---|---|---|---|
| 1 | Critical | Highest-priority emergency, such as heart attack | Priority Queue |
| 2 | Serious | Urgent case, such as house fire or burns | Priority Queue |
| 3 | Minor | Lower-priority case, such as minor accident | Regular Queue |

Lower severity numbers are processed first. Therefore, Severity 1 has the highest priority.

---

## Dispatch Workflow

```text
Incoming Emergency Call
        |
        v
Check Severity Level
        |
        |-- Severity 1 or 2 --> Priority Queue
        |
        |-- Severity 3 ------> Regular Queue
        |
        v
Check Available Ambulances
        |
        v
Process Priority Queue First
        |
        |-- If Priority Queue is empty --> Process Regular Queue
        |
        v
Use Graph + Dijkstra Algorithm
        |
        v
Find Nearest Available Ambulance
        |
        v
Dispatch Ambulance
        |
        v
Mark Ambulance as Busy
        |
        v
Complete Mission
        |
        v
Mark Ambulance as Available Again
