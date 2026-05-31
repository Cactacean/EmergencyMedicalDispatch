package com.example;

import java.util.Arrays;

/**
 *
 * Emergency Medical Dispatch System - Priority Queue Module
 *
 * Module : Priority Queue Class (by medical severity)
 *
 * Purpose:
 * Maintains a min-heap of incoming emergency calls ordered by
 * medical severity so that the most critical cases are always
 * dispatched first, regardless of the order they arrived.
 *
 * Severity levels (lower number = higher priority):
 *   1 – CRITICAL  (e.g. heart attack)
 *   2 – SERIOUS   (e.g. house fire with burns)
 *   3 – MINOR     (e.g. minor car accident)
 *
 * Tie-breaking rule:
 *   When two calls share the same severity, the earlier arrival
 *   is dispatched first (FIFO within the same tier).
 *
 * Data Structures Used:
 *   - EmergencyCall[]  : 1-indexed backing array for the heap
 *   - Min-heap         : parent.severity <= child.severity
 *
 * Time Complexities:
 *   enqueue  : O(log n)  – sift up from last leaf
 *   dequeue  : O(log n)  – sift down from root
 *   peek     : O(1)      – read index 1 (root)
 *   size     : O(1)
 * ---------------------------------------------------------------
 */
public class MedicalPriorityQueue {

    // ---------------------------------------------------------------
    // Backing heap array
    // 1-indexed so parent/child math stays clean:
    //   parent of i  -> i / 2
    //   left child   -> 2 * i
    //   right child  -> 2 * i + 1
    // ---------------------------------------------------------------
    private EmergencyCall[] heap;
    private int             size;
    private static final int DEFAULT_CAPACITY = 16;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /** Creates an empty queue with default capacity (16). */
    public MedicalPriorityQueue() {
        this(DEFAULT_CAPACITY);
    }

    /** Creates an empty queue with a custom initial capacity. */
    public MedicalPriorityQueue(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        heap = new EmergencyCall[initialCapacity + 1]; // index 0 unused
        size = 0;
    }

    // ---------------------------------------------------------------
    // Core operations
    // ---------------------------------------------------------------

    /**
     * Adds an emergency call to the queue.
     * Placed at the last leaf position, then sifted upward.
     * Time: O(log n)
     */
    public void enqueue(EmergencyCall call) {
        if (call == null) {
            throw new IllegalArgumentException("Cannot enqueue a null call.");
        }
        if (size + 1 == heap.length) {
            resize();
        }
        size++;
        heap[size] = call;
        siftUp(size);
    }

    /**
     * Removes and returns the highest-priority call (lowest severity number).
     * Root is replaced with the last leaf, then sifted downward.
     * Time: O(log n)
     *
     * @throws IllegalStateException if the queue is empty
     */
    public EmergencyCall dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException(
                    "Priority queue is empty. No call to dequeue.");
        }
        EmergencyCall highest = heap[1];
        heap[1]    = heap[size];
        heap[size] = null; // help GC
        size--;
        if (!isEmpty()) {
            siftDown(1);
        }
        return highest;
    }

    /**
     * Returns (without removing) the highest-priority call.
     * Time: O(1)
     *
     * @throws IllegalStateException if the queue is empty
     */
    public EmergencyCall peek() {
        if (isEmpty()) {
            throw new IllegalStateException(
                    "Priority queue is empty. No call to peek.");
        }
        return heap[1];
    }

    // ---------------------------------------------------------------
    // Heap maintenance helpers
    // ---------------------------------------------------------------

    /**
     * Sifts element at position i upward until heap property is restored.
     * Called after enqueue.
     */
    private void siftUp(int i) {
        while (i > 1) {
            int parent = i / 2;
            if (hasHigherPriority(heap[i], heap[parent])) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    /**
     * Sifts element at position i downward until heap property is restored.
     * Called after dequeue.
     */
    private void siftDown(int i) {
        while (true) {
            int left    = 2 * i;
            int right   = 2 * i + 1;
            int highest = i;

            if (left  <= size && hasHigherPriority(heap[left],  heap[highest]))
                highest = left;
            if (right <= size && hasHigherPriority(heap[right], heap[highest]))
                highest = right;

            if (highest != i) {
                swap(i, highest);
                i = highest;
            } else {
                break;
            }
        }
    }

    /**
     * Returns true when call 'a' should be served before call 'b':
     *   - Lower severity number wins (more critical), OR
     *   - Same severity and 'a' arrived earlier (FIFO tiebreak)
     */
    private boolean hasHigherPriority(EmergencyCall a, EmergencyCall b) {
        if (a.getSeverity() != b.getSeverity()) {
            return a.getSeverity() < b.getSeverity();
        }
        return a.getTimestamp() < b.getTimestamp();
    }

    /** Swaps elements at positions i and j in the heap array. */
    private void swap(int i, int j) {
        EmergencyCall temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    /** Doubles the capacity of the backing array when full. */
    private void resize() {
        heap = Arrays.copyOf(heap, heap.length * 2);
    }

    // ---------------------------------------------------------------
    // State queries
    // ---------------------------------------------------------------

    /** Returns true if the queue contains no elements. */
    public boolean isEmpty() { return size == 0; }

    /** Returns the number of calls currently in the queue. */
    public int size() { return size; }

    // ---------------------------------------------------------------
    // Display
    // ---------------------------------------------------------------

    /**
     * Prints queue contents from highest to lowest priority
     * by draining a copy. Does NOT modify the original queue.
     */
    public void display() {
        if (isEmpty()) {
            System.out.println("  [Priority Queue is empty]");
            return;
        }
        // Work on a copy so the real queue is untouched
        MedicalPriorityQueue copy = new MedicalPriorityQueue(heap.length - 1);
        copy.heap = Arrays.copyOf(heap, heap.length);
        copy.size = this.size;

        int rank = 1;
        while (!copy.isEmpty()) {
            System.out.printf("  %d. %s%n", rank++, copy.dequeue());
        }
    }

    // ---------------------------------------------------------------
    // Self-test — runs this module independently.
    // ---------------------------------------------------------------
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Priority Queue Module Self-Test ===");

        // -----------------------------------------------------------
        // Test 1: Project scenario – calls arrive in non-priority order
        //
        //   Call 1 – Heart attack     (Severity 1 – CRITICAL) at Location A
        //   Call 2 – Minor car accident (Severity 3 – MINOR)  at Location B
        //   Call 3 – House fire/burns (Severity 2 – SERIOUS)  at Location C
        //
        //   Expected dispatch order: Heart attack -> Fire -> Car accident
        // -----------------------------------------------------------
        System.out.println("\n-- Test 1: Assignment Scenario --");
        MedicalPriorityQueue pq = new MedicalPriorityQueue();

        pq.enqueue(new EmergencyCall("Heart Attack",          "A", 1));
        Thread.sleep(5);
        pq.enqueue(new EmergencyCall("Minor Car Accident",    "B", 3));
        Thread.sleep(5);
        pq.enqueue(new EmergencyCall("House Fire with Burns", "C", 2));

        pq.display();
        System.out.println("  Dispatching:");
        while (!pq.isEmpty()) {
            System.out.println("    Dispatched -> " + pq.dequeue());
        }

        // -----------------------------------------------------------
        // Test 2: Tie-breaking – same severity dispatches by arrival (FIFO)
        // -----------------------------------------------------------
        System.out.println("\n-- Test 2: Tie-breaking (same severity -> FIFO) --");
        MedicalPriorityQueue pq2 = new MedicalPriorityQueue();

        pq2.enqueue(new EmergencyCall("House Fire (first call)",  "D", 2));
        Thread.sleep(5);
        pq2.enqueue(new EmergencyCall("House Fire (second call)", "E", 2));
        Thread.sleep(5);
        pq2.enqueue(new EmergencyCall("Heart Attack",             "F", 1));

        pq2.display();
        System.out.println("  Dispatching:");
        while (!pq2.isEmpty()) {
            System.out.println("    Dispatched -> " + pq2.dequeue());
        }

        // -----------------------------------------------------------
        // Test 3: Dynamic arrivals – new call inserted mid-dispatch
        // -----------------------------------------------------------
        System.out.println("\n-- Test 3: Dynamic Arrivals --");
        MedicalPriorityQueue pq3 = new MedicalPriorityQueue();

        pq3.enqueue(new EmergencyCall("Minor Car Accident",    "G", 3));
        Thread.sleep(5);
        pq3.enqueue(new EmergencyCall("House Fire with Burns", "H", 2));

        System.out.println("    Dispatched -> " + pq3.dequeue());

        pq3.enqueue(new EmergencyCall("Heart Attack", "I", 1));
        System.out.println("  [New CRITICAL call arrived mid-dispatch]");
        pq3.display();
        while (!pq3.isEmpty()) {
            System.out.println("    Dispatched -> " + pq3.dequeue());
        }

        // -----------------------------------------------------------
        // Test 4: Edge cases
        // -----------------------------------------------------------
        System.out.println("\n-- Test 4: Edge Cases --");
        MedicalPriorityQueue pq4 = new MedicalPriorityQueue();
        System.out.println("  isEmpty on fresh queue : " + pq4.isEmpty());
        System.out.println("  size    on fresh queue : " + pq4.size());

        try {
            pq4.dequeue();
        } catch (IllegalStateException e) {
            System.out.println("  Caught expected (dequeue empty) : " + e.getMessage());
        }

        try {
            pq4.peek();
        } catch (IllegalStateException e) {
            System.out.println("  Caught expected (peek empty)    : " + e.getMessage());
        }

        try {
            new EmergencyCall("Bad Call", "Z", 5); // severity 5 is invalid
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught expected (bad severity)  : " + e.getMessage());
        }

        pq4.enqueue(new EmergencyCall("Heart Attack", "J", 1));
        System.out.println("  peek   : " + pq4.peek());
        System.out.println("  dequeue: " + pq4.dequeue());
        System.out.println("  isEmpty: " + pq4.isEmpty());

        System.out.println("\n=== All tests complete. ===");
    }
}