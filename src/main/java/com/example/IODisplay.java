package com.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * IODisplay - Main GUI Dashboard for Emergency Medical Dispatch System
 *
 * PURPOSE:
 * - Provides visual interface to demonstrate the dispatch system
 * - Shows ambulance status, emergency calls, and city map
 * - Animates ambulance movement along Dijkstra-calculated routes
 * - Runs 5 test scenarios covering all system behavior requirements
 */
public class IODisplay extends JFrame {

    // ======================== UI COMPONENTS ========================

    private JTextArea logArea;           // Displays dispatcher's "thinking" process
    private JTable ambulanceTable;       // Shows ambulance ID, location, status
    private JTable callTable;            // Shows emergency calls and their queues
    private JLabel statusBar;            // Bottom status messages
    private GraphPanel graphPanel;       // Custom panel that draws the city map

    private DefaultTableModel ambulanceModel;  // Data model for ambulance table
    private DefaultTableModel callModel;      // Data model for calls table

    //Backend component

    private DispatchSystem dispatchSystem;  // Main brain - connects all components
    private Graph cityGraph;               // City map with roads and travel times
    private List<Ambulance> ambulances;    // List of all ambulances in system

    //Route animation tracking

    private List<String> currentRoute = new ArrayList<>();  // Path ambulance will follow
    private int routeStepIndex = 0;          // Current step in the route (0 = start)
    private String currentAmbulanceId = "";  // Which ambulance is moving
    private Point ambulancePosition = null;  // Current (x,y) coordinates on map

    // Stores pre-defined data for all 5 scenarios (title, calls, routes, etc.)
    private Map<Integer, ScenarioData> scenarioData = new HashMap<>();

    /**
     * CONSTRUCTOR - Sets up the entire GUI window and all its components
     * Called once when the program starts
     */
    public IODisplay() {
        // Window settings
        setTitle("🚨 Emergency Dispatch System - Simulation Dashboard");
        setSize(1400, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize backend systems (Graph, DispatchSystem, Ambulances)
        initializeBackend();

        // Load scenario data into memory (defines what each scenario does)
        loadScenarioData();

        //LOG AREA
        logArea = new JTextArea();
        logArea.setEditable(false);                    // User cannot type here
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));  // Fixed-width font
        logArea.setBackground(new Color(250, 250, 250));
        logArea.setForeground(Color.BLACK);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(400, 600));
        logScroll.setBorder(BorderFactory.createTitledBorder("📋 Dispatch Log"));

        //AMBULANCE TABLE
        String[] ambCols = {"🚑 ID", "📍 Location", "🟢 Status"};
        ambulanceModel = new DefaultTableModel(ambCols, 0);
        ambulanceTable = new JTable(ambulanceModel);
        ambulanceTable.setRowHeight(25);
        ambulanceTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ambulanceTable.setForeground(Color.BLACK);

        // Custom renderer for status column - changes colors based on AVAILABLE/BUSY
        ambulanceTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setForeground(Color.BLACK);
                if (column == 2) { // Status column
                    String status = value != null ? value.toString() : "";
                    if (status.contains("AVAILABLE")) {
                        c.setBackground(new Color(144, 238, 144)); // Light green - available
                        setText("🟢 AVAILABLE");
                    } else if (status.contains("BUSY")) {
                        c.setBackground(Color.BLACK);              // Black background for busy
                        c.setForeground(Color.WHITE);              // White text for contrast
                        setText("🔴 BUSY");
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        //CALL TABLE- shows emergency calls)
        String[] callCols = {"📞 Description", "📍 Location", "⚠️ Severity", "📊 Queue"};
        callModel = new DefaultTableModel(callCols, 0);
        callTable = new JTable(callModel);
        callTable.setRowHeight(25);
        callTable.setForeground(Color.BLACK);

        // Custom renderer for severity column - color codes by severity level
        callTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setForeground(Color.BLACK);
                if (column == 2 && value != null) { // Severity column
                    try {
                        int severity = Integer.parseInt(value.toString());
                        if (severity == 1) {
                            c.setBackground(new Color(255, 200, 200)); // Light red - CRITICAL
                        } else if (severity == 2) {
                            c.setBackground(new Color(255, 220, 180)); // Light orange - SERIOUS
                        } else {
                            c.setBackground(new Color(200, 255, 200)); // Light green - MINOR
                        }
                    } catch (NumberFormatException e) {
                        c.setBackground(Color.WHITE);
                    }
                } else if (column == 3 && value != null) { // Queue column
                    String queue = value.toString();
                    if (queue.contains("Priority")) {
                        c.setBackground(new Color(255, 200, 200)); // Priority Queue = red tint
                    } else {
                        c.setBackground(new Color(200, 255, 200)); // Regular Queue = green tint
                    }
                } else {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        //TABLES PANEL
        JPanel tablesPanel = new JPanel(new BorderLayout());
        tablesPanel.setBorder(BorderFactory.createTitledBorder("📊 System Status"));

        // Split pane allows resizing between ambulance table and call table
        JSplitPane tableSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(ambulanceTable), new JScrollPane(callTable));
        tableSplit.setDividerLocation(200);  // Give ambulance table 200px, rest to call table
        tablesPanel.add(tableSplit, BorderLayout.CENTER);

        //GRAPH PANEL
        graphPanel = new GraphPanel();
        graphPanel.setPreferredSize(new Dimension(550, 500));
        graphPanel.setBorder(BorderFactory.createTitledBorder("🗺️ City Map - Ambulance Route"));
        graphPanel.setBackground(Color.WHITE);

        //MAIN CONTENT PANEL
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tablesPanel, BorderLayout.WEST);
        mainPanel.add(graphPanel, BorderLayout.CENTER);

        //7. LOG PANEL
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.add(logScroll, BorderLayout.CENTER);
        logPanel.setPreferredSize(new Dimension(450, 600));
        logPanel.setBorder(BorderFactory.createTitledBorder("📝 System Log"));

        //MAIN SPLIT
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPanel, logPanel);
        mainSplit.setDividerLocation(600);  // Give main panel 600px, log gets the rest

        //STATUS BAR
        statusBar = new JLabel("✅ System Ready");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusBar.setForeground(Color.BLACK);
        statusBar.setBackground(new Color(240, 240, 240));
        statusBar.setOpaque(true);

        //BUTTON PANEL
        JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 10, 8));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setBackground(new Color(240, 240, 240));

        // Create all buttons with custom styling
        JButton s1Btn = createStyledButton("📋 Scenario 1", new Color(70, 130, 200), Color.BLACK);
        JButton s2Btn = createStyledButton("📋 Scenario 2", new Color(70, 130, 200), Color.BLACK);
        JButton s3Btn = createStyledButton("📋 Scenario 3", new Color(70, 130, 200), Color.BLACK);
        JButton s4Btn = createStyledButton("📋 Scenario 4", new Color(70, 130, 200), Color.BLACK);
        JButton s5Btn = createStyledButton("📋 Scenario 5", new Color(70, 130, 200), Color.BLACK);
        JButton nextBtn = createStyledButton("▶ Next Step", new Color(34, 139, 34), Color.BLACK);
        JButton resetBtn = createStyledButton("🔄 Reset System", new Color(200, 100, 100), Color.BLACK);
        JButton clearLogBtn = createStyledButton("🗑️ Clear Log", new Color(150, 150, 150), Color.BLACK);

        // Add buttons to panel
        buttonPanel.add(s1Btn);
        buttonPanel.add(s2Btn);
        buttonPanel.add(s3Btn);
        buttonPanel.add(s4Btn);
        buttonPanel.add(s5Btn);
        buttonPanel.add(nextBtn);
        buttonPanel.add(resetBtn);
        buttonPanel.add(clearLogBtn);

        //FINAL LAYOUT ASSEMBLY
        add(buttonPanel, BorderLayout.NORTH);   // Buttons at top
        add(mainSplit, BorderLayout.CENTER);     // Tables + Map in middle
        add(statusBar, BorderLayout.SOUTH);      // Status bar at bottom

        // Set overall background color
        getContentPane().setBackground(new Color(240, 240, 240));

        //BUTTON ACTIONS
        s1Btn.addActionListener(e -> runScenario(1));   // Lambda expression - runs scenario 1
        s2Btn.addActionListener(e -> runScenario(2));
        s3Btn.addActionListener(e -> runScenario(3));
        s4Btn.addActionListener(e -> runScenario(4));
        s5Btn.addActionListener(e -> runScenario(5));
        nextBtn.addActionListener(e -> advanceAmbulance());  // Move ambulance one step
        resetBtn.addActionListener(e -> resetSystem());       // Reset everything
        clearLogBtn.addActionListener(e -> logArea.setText("")); // Clear log text
    }

    /**
     * Helper method to create consistently styled buttons
     * @param text Button label
     * @param bgColor Background color
     * @param fgColor Text color
     * @return Styled JButton
     */
    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);  // Remove focus border
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));  // Padding
        btn.setOpaque(true);
        return btn;
    }

    /**
     * Initializes backend systems:
     * - Creates city graph with roads and travel times
     * - Creates DispatchSystem (main coordinator)
     * - Creates two ambulances at Base1 and Base2
     */
    private void initializeBackend() {
        cityGraph = buildCityGraph();           // Build map with Dijkstra-ready graph
        dispatchSystem = new DispatchSystem(cityGraph);

        ambulances = new ArrayList<>();
        Ambulance amb1 = new Ambulance("AMB-01", "Base1");
        Ambulance amb2 = new Ambulance("AMB-02", "Base2");
        ambulances.add(amb1);
        ambulances.add(amb2);

        // Register ambulances with dispatch system
        for (Ambulance amb : ambulances) {
            dispatchSystem.addAmbulance(amb);
        }
    }

    /**
     * Builds the city graph with all roads and travel times (in minutes)
     * Edge weights = travel time in minutes
     */
    private Graph buildCityGraph() {
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

    /**
     * Loads pre-defined data for all 5 test scenarios
     * Each scenario tests different system behaviors
     */
    private void loadScenarioData() {
        // SCENARIO 1: Priority ordering + Dijkstra routing
        // Tests: Severity 1 > 2 > 3 ordering, Dijkstra finds shortest path
        scenarioData.put(1, new ScenarioData(
                "Priority Ordering + Dijkstra",
                "Calls arrive in non-priority order. PQ reorders by severity.",
                Arrays.asList("Heart Attack", "Minor Car Accident", "House Fire"),
                Arrays.asList("A", "B", "C"),
                Arrays.asList(1, 3, 2),  // Severities: 1=CRITICAL, 2=SERIOUS, 3=MINOR
                Arrays.asList("Priority Queue", "Regular Queue", "Priority Queue"),
                Arrays.asList("Base1", "A"),  // Route: Base1 → A
                "AMB-01"
        ));

        // SCENARIO 2: Both ambulances busy
        // Tests: Calls queue when no ambulances available
        scenarioData.put(2, new ScenarioData(
                "Both Ambulances Busy",
                "Both ambulances start BUSY. Calls wait in queues.",
                Arrays.asList("House Fire", "Minor Car Accident"),
                Arrays.asList("C", "B"),
                Arrays.asList(2, 3),
                Arrays.asList("Priority Queue", "Regular Queue"),
                Collections.emptyList(),  // No route (nothing dispatched)
                ""
        ));

        // SCENARIO 3: Priority Queue checked first
        // Tests: When ambulance freed, check Priority Queue BEFORE Regular Queue
        scenarioData.put(3, new ScenarioData(
                "Priority Queue Checked First",
                "Ambulance freed → checks Priority Queue before Regular Queue.",
                Arrays.asList("House Fire", "Minor Car Accident"),
                Arrays.asList("C", "B"),
                Arrays.asList(2, 3),
                Arrays.asList("Priority Queue", "Regular Queue"),
                Arrays.asList("Base1", "A", "C"),  // Route: Base1 → A → C
                "AMB-01"
        ));

        // SCENARIO 4: FIFO tie-breaking
        // Tests: Same severity = earlier call wins (FIFO within same tier)
        scenarioData.put(4, new ScenarioData(
                "FIFO Tie-breaking",
                "Same severity → earlier arrival dispatched first.",
                Arrays.asList("House Fire #1", "House Fire #2", "Heart Attack"),
                Arrays.asList("C", "D", "A"),
                Arrays.asList(2, 2, 1),
                Arrays.asList("Priority Queue", "Priority Queue", "Priority Queue"),
                Arrays.asList("Base2", "B", "D"),  // Route: Base2 → B → D
                "AMB-02"
        ));

        // SCENARIO 5: All non-urgent calls
        // Tests: Severity 3 → Regular Queue only, pure FIFO order
        scenarioData.put(5, new ScenarioData(
                "All Non-Urgent Calls",
                "All severity 3 → Regular Queue FIFO order.",
                Arrays.asList("Broken Leg", "Minor Bruising", "Twisted Ankle"),
                Arrays.asList("B", "D", "C"),
                Arrays.asList(3, 3, 3),
                Arrays.asList("Regular Queue", "Regular Queue", "Regular Queue"),
                Arrays.asList("Base1", "A", "B"),  // Route: Base1 → A → B
                "AMB-01"
        ));
    }

    /**
     * Runs a specific scenario by number (1-5)
     * Resets system first, then loads scenario data and simulates
     *
     * @param scenarioNum Which scenario to run (1-5)
     */
    private void runScenario(int scenarioNum) {
        resetSystem();  // Clear previous data

        ScenarioData data = scenarioData.get(scenarioNum);
        if (data == null) return;

        // Log scenario header
        logArea.append("\n" + "═".repeat(70) + "\n");
        logArea.append(String.format("🎯 SCENARIO %d: %s\n", scenarioNum, data.title));
        logArea.append("═".repeat(70) + "\n");
        logArea.append("📌 " + data.description + "\n\n");

        // Setup ambulance table based on scenario
        ambulanceModel.setRowCount(0);

        if (scenarioNum == 2) {
            // Scenario 2: Both ambulances start BUSY
            for (Ambulance amb : ambulances) {
                amb.dispatch();  // Set status to busy
                ambulanceModel.addRow(new Object[]{amb.getId(), amb.getCurrentLocation(), "BUSY"});
            }
            logArea.append("⚠️ Both ambulances are BUSY at start.\n");
        } else {
            // Other scenarios: Both ambulances start AVAILABLE
            for (Ambulance amb : ambulances) {
                ambulanceModel.addRow(new Object[]{amb.getId(), amb.getCurrentLocation(), "AVAILABLE"});
            }
        }

        // Log and add incoming calls
        logArea.append("\n📞 INCOMING CALLS:\n");
        logArea.append("─".repeat(50) + "\n");

        callModel.setRowCount(0);  // Clear previous calls
        for (int i = 0; i < data.callDescriptions.size(); i++) {
            // Create emergency call object
            EmergencyCall call = new EmergencyCall(
                    data.callDescriptions.get(i),
                    data.callLocations.get(i),
                    data.severities.get(i)
            );
            dispatchSystem.receiveCall(call);  // Process through dispatch system

            String queueType = data.queueTypes.get(i);
            String severityLabel = getSeverityLabel(data.severities.get(i));

            // Add to UI table
            callModel.addRow(new Object[]{
                    data.callDescriptions.get(i),
                    data.callLocations.get(i),
                    data.severities.get(i),
                    queueType
            });

            // Log the call
            logArea.append(String.format("  📞 %s at %s (Severity %d - %s) → %s\n",
                    data.callDescriptions.get(i),
                    data.callLocations.get(i),
                    data.severities.get(i),
                    severityLabel,
                    queueType
            ));
        }

        // Setup route for animation (if scenario has a route)
        if (!data.route.isEmpty()) {
            currentRoute = new ArrayList<>(data.route);
            currentAmbulanceId = data.ambulanceId;
            routeStepIndex = 0;

            logArea.append("\n🚑 Selected Ambulance: " + currentAmbulanceId + "\n");
            logArea.append("🗺️ Route: " + String.join(" → ", currentRoute) + "\n");
            logArea.append("\n💡 Press 'Next Step' to watch the ambulance travel along the route!\n");

            // Show starting position on map
            ambulancePosition = GraphPanel.getNodeCoordinates(currentRoute.get(0));
            graphPanel.setAmbulancePosition(ambulancePosition);
            graphPanel.setRoute(currentRoute);
            graphPanel.setCurrentStep(0);
        } else {
            currentRoute = new ArrayList<>();
            routeStepIndex = 0;
            graphPanel.setAmbulancePosition(null);
            graphPanel.setRoute(Collections.emptyList());
        }

        // Special handling for Scenario 3: Free ambulance to demonstrate PQ first
        if (scenarioNum == 3) {
            logArea.append("\n🔄 SCENARIO 3: Freeing AMB-01 to demonstrate Priority Queue first...\n");
            Ambulance amb1 = ambulances.get(0);
            amb1.completeMission("Base1");  // Make available again
            ambulanceModel.setValueAt("AVAILABLE", 0, 2);
            ambulanceModel.setValueAt("Base1", 0, 1);
            dispatchSystem.processCalls();  // Should pick from PQ first
            logArea.append("✅ Priority Queue checked first! House Fire dispatched.\n");
        }

        logArea.append("\n" + "═".repeat(70) + "\n");
        updateAmbulanceTable();
        statusBar.setText(String.format("✅ Scenario %d loaded. Press 'Next Step' to animate ambulance.", scenarioNum));
        graphPanel.repaint();  // Redraw map
    }

    /**
     * Moves ambulance one step along the route
     * Called when user clicks "Next Step" button
     * Shows travel time between nodes
     */
    private void advanceAmbulance() {
        if (currentRoute.isEmpty()) {
            statusBar.setText("⚠️ No active route. Please load a scenario first.");
            return;
        }

        if (routeStepIndex < currentRoute.size()) {
            String currentNode = currentRoute.get(routeStepIndex);
            String nextNode = (routeStepIndex + 1 < currentRoute.size()) ?
                    currentRoute.get(routeStepIndex + 1) : null;

            logArea.append(String.format("\n🚑 Ambulance %s moving from %s",
                    currentAmbulanceId, currentNode));

            if (nextNode != null) {
                logArea.append(String.format(" to %s...\n", nextNode));

                // Look up travel time between nodes
                int eta = getEdgeWeight(currentNode, nextNode);
                if (eta > 0) {
                    logArea.append(String.format("   ⏱️ Travel time: %d minutes\n", eta));
                }
            } else {
                // Reached destination!
                logArea.append(" (DESTINATION REACHED!)\n");
                logArea.append("🎉 Ambulance has arrived at the emergency scene!\n");
                statusBar.setText("🎉 Ambulance arrived at destination! Mission complete.");

                // Update ambulance location in table
                for (int i = 0; i < ambulanceModel.getRowCount(); i++) {
                    if (ambulanceModel.getValueAt(i, 0).equals(currentAmbulanceId)) {
                        ambulanceModel.setValueAt(currentNode, i, 1);
                        break;
                    }
                }
            }

            routeStepIndex++;
            graphPanel.setCurrentStep(routeStepIndex);
            graphPanel.repaint();

            // Update status bar with current position
            if (routeStepIndex < currentRoute.size()) {
                statusBar.setText(String.format("🚑 Ambulance at %s → Next: %s",
                        currentNode, currentRoute.get(routeStepIndex)));
            } else {
                statusBar.setText("🏁 Mission complete! Ambulance has arrived.");
            }
        } else {
            logArea.append("\n🏁 Journey complete! Ambulance has already reached the destination.\n");
            statusBar.setText("🏁 Mission already complete. Load another scenario or reset.");
        }
    }

    /**
     * Gets travel time (edge weight) between two locations
     * Used for displaying ETA during animation
     *
     * @param from Starting location
     * @param to Destination location
     * @return Travel time in minutes, or 0 if not found
     */
    private int getEdgeWeight(String from, String to) {
        Map<String, Integer> edges = new HashMap<>();
        // All roads are bidirectional, so store both directions
        edges.put("Base1-A", 4);
        edges.put("Base1-Base2", 7);
        edges.put("A-B", 3);
        edges.put("A-C", 2);
        edges.put("B-Base2", 5);
        edges.put("B-D", 6);
        edges.put("C-D", 5);
        // Reverse directions (bidirectional roads)
        edges.put("A-Base1", 4);
        edges.put("Base2-Base1", 7);
        edges.put("B-A", 3);
        edges.put("C-A", 2);
        edges.put("Base2-B", 5);
        edges.put("D-B", 6);
        edges.put("D-C", 5);

        String key = from + "-" + to;
        return edges.getOrDefault(key, 0);
    }

    /**
     * Converts severity number to human-readable label
     * @param severity 1, 2, or 3
     * @return "CRITICAL", "SERIOUS", "MINOR", or "UNKNOWN"
     */
    private String getSeverityLabel(int severity) {
        switch(severity) {
            case 1: return "CRITICAL";
            case 2: return "SERIOUS";
            case 3: return "MINOR";
            default: return "UNKNOWN";
        }
    }

    /**
     * Resets entire system to initial state
     * Clears tables, resets ambulances, clears routes
     */
    private void resetSystem() {
        // Clear UI tables
        ambulanceModel.setRowCount(0);
        callModel.setRowCount(0);

        // Reinitialize backend (fresh state)
        initializeBackend();

        // Update ambulance table with fresh data
        for (Ambulance amb : ambulances) {
            ambulanceModel.addRow(new Object[]{amb.getId(), amb.getCurrentLocation(), "AVAILABLE"});
        }

        // Clear route animation data
        currentRoute = new ArrayList<>();
        routeStepIndex = 0;
        currentAmbulanceId = "";
        ambulancePosition = null;
        graphPanel.setAmbulancePosition(null);
        graphPanel.setRoute(Collections.emptyList());
        graphPanel.setCurrentStep(0);
        graphPanel.repaint();

        statusBar.setText("✅ System reset. Select a scenario to begin.");
        logArea.append("\n🔄 System has been reset.\n");
    }

    /**
     * Updates ambulance table with current status from backend
     * Called after state changes to keep UI in sync
     */
    private void updateAmbulanceTable() {
        for (int i = 0; i < ambulances.size(); i++) {
            Ambulance amb = ambulances.get(i);
            String status = amb.isAvailable() ? "AVAILABLE" : "BUSY";
            ambulanceModel.setValueAt(status, i, 2);
            ambulanceModel.setValueAt(amb.getCurrentLocation(), i, 1);
        }
    }

    /**
     * Inner class to store pre-defined scenario data
     * Makes it easy to add/modify scenarios
     */
    private static class ScenarioData {
        String title;                    // Scenario name
        String description;              // What this scenario tests
        List<String> callDescriptions;   // Names of emergency calls
        List<String> callLocations;      // Where each call occurs
        List<Integer> severities;        // Severity level for each call
        List<String> queueTypes;         // Which queue each call goes to
        List<String> route;              // Path ambulance will take
        String ambulanceId;              // Which ambulance is dispatched

        ScenarioData(String title, String description, List<String> callDescriptions,
                     List<String> callLocations, List<Integer> severities,
                     List<String> queueTypes, List<String> route, String ambulanceId) {
            this.title = title;
            this.description = description;
            this.callDescriptions = callDescriptions;
            this.callLocations = callLocations;
            this.severities = severities;
            this.queueTypes = queueTypes;
            this.route = route;
            this.ambulanceId = ambulanceId;
        }
    }

    /**
     * MAIN METHOD - Application entry point
     * Creates and displays the GUI on the Event Dispatch Thread (EDT)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use system look and feel for native appearance
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            IODisplay gui = new IODisplay();
            gui.setVisible(true);  // Show the window
        });
    }
}

// GRAPH PANEL - Custom component that draws the city map

/**
 * GraphPanel - Custom JPanel that draws the city map and animates ambulance movement
 *
 * This class is responsible for:
 * - Drawing roads (edges) between locations
 * - Displaying travel times (weights) on each road
 * - Highlighting the ambulance's planned route in red
 * - Drawing the ambulance icon (🚑) at its current position
 * - Handling coordinate mapping (node names → (x,y) positions)
 */
class GraphPanel extends JPanel {
    private List<String> route = new ArrayList<>();  // Planned route for ambulance
    private int currentStep = 0;                     // Current position in route
    private Point ambulancePosition = null;          // Current (x,y) on screen

    /**
     * Maps location names to screen coordinates (x,y)
     * These positions are hardcoded for consistent layout
     *
     * Layout:
     *        C(200,100)    D(300,100)
     *            |            |
     *            |            |
     *        A(200,200)    B(300,200)
     *            |            |
     *            |            |
     *     Base1(100,300)  Base2(400,300)
     */
    private static final Map<String, Point> NODE_COORDS = new HashMap<>();

    static {
        NODE_COORDS.put("Base1", new Point(100, 300));
        NODE_COORDS.put("Base2", new Point(400, 300));
        NODE_COORDS.put("A", new Point(200, 200));
        NODE_COORDS.put("B", new Point(300, 200));
        NODE_COORDS.put("C", new Point(200, 100));
        NODE_COORDS.put("D", new Point(300, 100));
    }

    /**
     * Gets screen coordinates for a given node name
     * Used by IODisplay to position ambulance on map
     */
    public static Point getNodeCoordinates(String node) {
        return NODE_COORDS.get(node);
    }

    /**
     * Sets the route to be highlighted on map
     * Also resets animation to first step
     */
    public void setRoute(List<String> route) {
        this.route = route;
        this.currentStep = 0;
        if (!route.isEmpty()) {
            this.ambulancePosition = NODE_COORDS.get(route.get(0));
        }
        repaint();  // Request redraw
    }

    /**
     * Updates which step of the route we're on
     * Moves ambulance icon to new position
     */
    public void setCurrentStep(int step) {
        this.currentStep = step;
        if (!route.isEmpty() && step < route.size()) {
            this.ambulancePosition = NODE_COORDS.get(route.get(step));
        } else if (!route.isEmpty() && step >= route.size()) {
            this.ambulancePosition = NODE_COORDS.get(route.get(route.size() - 1));
        }
        repaint();
    }

    /**
     * Directly sets ambulance position (without route)
     */
    public void setAmbulancePosition(Point pos) {
        this.ambulancePosition = pos;
        repaint();
    }

    /**
     * MAIN DRAWING METHOD - Called automatically when repaint() is called
     * This is where all graphics rendering happens
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // Enable anti-aliasing for smoother lines and text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setBackground(Color.WHITE);

        //DRAW ALL ROADS
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(2));

        drawEdge(g2d, "Base1", "A", "4");
        drawEdge(g2d, "Base1", "Base2", "7");
        drawEdge(g2d, "A", "B", "3");
        drawEdge(g2d, "A", "C", "2");
        drawEdge(g2d, "B", "Base2", "5");
        drawEdge(g2d, "B", "D", "6");
        drawEdge(g2d, "C", "D", "5");

        //DRAW ROUTE HIGHLIGHT (red path for ambulance)
        if (route != null && route.size() > 1) {
            g2d.setColor(new Color(255, 0, 0, 80));  // Semi-transparent red
            g2d.setStroke(new BasicStroke(4));
            for (int i = 0; i < route.size() - 1; i++) {
                Point p1 = NODE_COORDS.get(route.get(i));
                Point p2 = NODE_COORDS.get(route.get(i + 1));
                if (p1 != null && p2 != null) {
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        //DRAW LOCATION NODES
        for (String node : NODE_COORDS.keySet()) {
            Point p = NODE_COORDS.get(node);

            // Draw blue circle background
            g2d.setColor(new Color(200, 220, 255));  // Light blue
            g2d.fillOval(p.x - 20, p.y - 20, 40, 40);
            g2d.setColor(new Color(50, 50, 150));    // Dark blue border
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(p.x - 20, p.y - 20, 40, 40);

            // Draw location name in black text
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2d.drawString(node, p.x - 15, p.y + 25);
        }

        //4. DRAW AMBULANCE ICON
        if (ambulancePosition != null) {
            // Red background circle
            g2d.setColor(new Color(255, 80, 80));
            g2d.fillOval(ambulancePosition.x - 12, ambulancePosition.y - 12, 24, 24);

            // Ambulance emoji
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2d.drawString("🚑", ambulancePosition.x - 8, ambulancePosition.y + 5);

            // Red border
            g2d.setColor(new Color(180, 0, 0));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(ambulancePosition.x - 12, ambulancePosition.y - 12, 24, 24);
        }
    }

    /**
     * Helper method to draw a single road between two locations
     * Draws line and travel time label in the middle
     */
    private void drawEdge(Graphics2D g, String from, String to, String weight) {
        Point p1 = NODE_COORDS.get(from);
        Point p2 = NODE_COORDS.get(to);
        if (p1 != null && p2 != null) {
            // Draw the road line
            g.drawLine(p1.x, p1.y, p2.x, p2.y);

            // Calculate middle point for weight label
            int mx = (p1.x + p2.x) / 2;
            int my = (p1.y + p2.y) / 2;

            // Draw weight label in black
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString(weight + "m", mx - 5, my + 4);
        }
    }
}
