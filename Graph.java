import java.util.*;

/**
 * Graph.java
 * ---------------------------------------------------------------
 * Emergency Medical Dispatch System - Graph Module
 *
 * Author : Jeff
 * Module : Graph Class + Dijkstra's Shortest Path Algorithm
 *
 * Purpose:
 *   Represents the city's road network as a weighted undirected
 *   graph. Each node is an intersection / location (hospital,
 *   ambulance base, or emergency site). Each edge stores the
 *   travel time (in minutes) between two adjacent nodes.
 *
 *   Dijkstra's algorithm is used to compute the shortest travel
 *   time (ETA) from any ambulance's current location to an
 *   emergency call location, so the dispatcher can assign the
 *   nearest available ambulance.
 *
 * Data Structures Used:
 *   - HashMap<String, List<Edge>>  : adjacency list
 *   - PriorityQueue<NodeDistance>  : min-heap for Dijkstra
 *   - HashMap<String, Integer>     : distance table
 *   - HashMap<String, String>      : predecessor table (for path)
 * ---------------------------------------------------------------
 */
public class Graph {

    // ---------------------------------------------------------------
    // Inner class: Edge
    // Represents a road segment between two locations with a weight
    // (travel time in minutes).
    // ---------------------------------------------------------------
    public static class Edge {
        String destination;
        int weight; // travel time in minutes

        public Edge(String destination, int weight) {
            this.destination = destination;
            this.weight = weight;
        }
    }

    // ---------------------------------------------------------------
    // Inner class: NodeDistance
    // Helper class used inside Dijkstra's priority queue to order
    // nodes by their current shortest known distance.
    // ---------------------------------------------------------------
    private static class NodeDistance implements Comparable<NodeDistance> {
        String node;
        int distance;

        public NodeDistance(String node, int distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    // ---------------------------------------------------------------
    // Inner class: PathResult
    // Holds the result of a Dijkstra computation: the total ETA and
    // the ordered list of nodes from source to destination.
    // ---------------------------------------------------------------
    public static class PathResult {
        public int eta;                // total travel time in minutes
        public List<String> path;      // ordered route

        public PathResult(int eta, List<String> path) {
            this.eta = eta;
            this.path = path;
        }

        @Override
        public String toString() {
            if (eta == Integer.MAX_VALUE) {
                return "No route available.";
            }
            return "ETA = " + eta + " min | Route: " + String.join(" -> ", path);
        }
    }

    // ---------------------------------------------------------------
    // Adjacency list: maps each node name to its list of edges.
    // Using HashMap gives O(1) average lookup of a node's neighbours.
    // ---------------------------------------------------------------
    private final Map<String, List<Edge>> adjacencyList;

    /** Construct an empty graph. */
    public Graph() {
        this.adjacencyList = new HashMap<>();
    }

    /**
     * Add a node (location) to the graph.
     * Safe to call multiple times - duplicates are ignored.
     */
    public void addNode(String name) {
        adjacencyList.putIfAbsent(name, new ArrayList<>());
    }

    /**
     * Add an undirected edge between two nodes with a given weight
     * (travel time in minutes). Roads are assumed bidirectional.
     *
     * @param from   source node
     * @param to     destination node
     * @param weight travel time in minutes (must be >= 0 for Dijkstra)
     */
    public void addEdge(String from, String to, int weight) {
        if (weight < 0) {
            throw new IllegalArgumentException(
                "Dijkstra's algorithm does not support negative weights.");
        }
        addNode(from);
        addNode(to);
        adjacencyList.get(from).add(new Edge(to, weight));
        adjacencyList.get(to).add(new Edge(from, weight)); // undirected
    }

    /** Returns true if the graph contains the given node. */
    public boolean hasNode(String name) {
        return adjacencyList.containsKey(name);
    }

    /** Returns all node names in the graph. */
    public Set<String> getNodes() {
        return adjacencyList.keySet();
    }

    // ===============================================================
    // DIJKSTRA'S SHORTEST PATH ALGORITHM
    // ---------------------------------------------------------------
    // Computes the shortest travel time from 'source' to 'target'
    // using a min-heap (PriorityQueue).
    //
    // Time complexity : O((V + E) log V)
    //   - V = number of nodes, E = number of edges
    //   - Each node is polled from the heap once -> V log V
    //   - Each edge is relaxed at most once       -> E log V
    //
    // Space complexity: O(V + E)
    //   - O(V) for distance and predecessor maps
    //   - O(V) for the heap (at most one entry per node at a time
    //     in the "decrease-key by re-insertion" pattern, total
    //     pushes <= E, so heap size is O(E) in the worst case)
    // ===============================================================
    public PathResult dijkstra(String source, String target) {

        // Defensive checks
        if (!adjacencyList.containsKey(source)) {
            throw new IllegalArgumentException("Source node not in graph: " + source);
        }
        if (!adjacencyList.containsKey(target)) {
            throw new IllegalArgumentException("Target node not in graph: " + target);
        }

        // distance[v] = shortest known distance from source to v
        Map<String, Integer> distance = new HashMap<>();
        // predecessor[v] = the node we came from on the shortest path to v
        Map<String, String> predecessor = new HashMap<>();

        // Initialise: every node starts at infinity, source at 0
        for (String node : adjacencyList.keySet()) {
            distance.put(node, Integer.MAX_VALUE);
        }
        distance.put(source, 0);

        // Min-heap ordered by current shortest distance
        PriorityQueue<NodeDistance> minHeap = new PriorityQueue<>();
        minHeap.offer(new NodeDistance(source, 0));

        // Track which nodes have been finalised (shortest path found)
        Set<String> visited = new HashSet<>();

        while (!minHeap.isEmpty()) {
            NodeDistance current = minHeap.poll();
            String u = current.node;

            // Skip if we already settled this node with a smaller distance
            if (!visited.add(u)) {
                continue;
            }

            // Early termination: once we settle the target, we are done
            if (u.equals(target)) {
                break;
            }

            // Relax all outgoing edges from u
            for (Edge edge : adjacencyList.get(u)) {
                String v = edge.destination;
                if (visited.contains(v)) continue;

                int newDist = distance.get(u) + edge.weight;
                if (newDist < distance.get(v)) {
                    distance.put(v, newDist);
                    predecessor.put(v, u);
                    minHeap.offer(new NodeDistance(v, newDist));
                }
            }
        }

        int finalEta = distance.get(target);
        List<String> path = reconstructPath(predecessor, source, target, finalEta);
        return new PathResult(finalEta, path);
    }

    /**
     * Helper: rebuild the shortest path from source -> target using
     * the predecessor map produced by Dijkstra.
     */
    private List<String> reconstructPath(Map<String, String> predecessor,
                                         String source,
                                         String target,
                                         int finalEta) {
        List<String> path = new ArrayList<>();
        if (finalEta == Integer.MAX_VALUE) {
            return path; // unreachable
        }
        String step = target;
        while (step != null) {
            path.add(0, step);
            if (step.equals(source)) break;
            step = predecessor.get(step);
        }
        return path;
    }

    /**
     * Convenience method used by the dispatcher:
     * Given a set of candidate ambulance locations, return the one
     * with the shortest ETA to the given emergency location.
     *
     * @param ambulanceLocations map of ambulanceId -> currentNode
     * @param emergencyNode      location of the emergency call
     * @return an Object[] containing { ambulanceId, PathResult }
     *         or null if no ambulance can reach the emergency.
     */
    public Object[] findNearestAmbulance(Map<String, String> ambulanceLocations,
                                         String emergencyNode) {
        String bestAmbulance = null;
        PathResult bestResult = null;

        for (Map.Entry<String, String> entry : ambulanceLocations.entrySet()) {
            String ambulanceId = entry.getKey();
            String ambulanceNode = entry.getValue();

            PathResult result = dijkstra(ambulanceNode, emergencyNode);
            if (result.eta == Integer.MAX_VALUE) continue;

            if (bestResult == null || result.eta < bestResult.eta) {
                bestResult = result;
                bestAmbulance = ambulanceId;
            }
        }

        if (bestAmbulance == null) return null;
        return new Object[] { bestAmbulance, bestResult };
    }

    // ---------------------------------------------------------------
    // Quick self-test for Jeff's module (run independently).
    // The teammates' Main class will use Graph in the full system.
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        Graph city = new Graph();

        // Build a small sample city map:
        //
        //        (4)        (2)
        //   Base1 --- A --------- C
        //     |       |           |
        //   (7)     (3)         (5)
        //     |       |           |
        //   Base2 --- B --------- D
        //                (6)
        //
        city.addEdge("Base1", "A", 4);
        city.addEdge("Base1", "Base2", 7);
        city.addEdge("A", "B", 3);
        city.addEdge("A", "C", 2);
        city.addEdge("B", "Base2", 5);
        city.addEdge("B", "D", 6);
        city.addEdge("C", "D", 5);

        System.out.println("=== Graph Module Self-Test ===");

        // Test 1: shortest path from Base1 to D
        PathResult r1 = city.dijkstra("Base1", "D");
        System.out.println("Base1 -> D : " + r1);

        // Test 2: shortest path from Base2 to C
        PathResult r2 = city.dijkstra("Base2", "C");
        System.out.println("Base2 -> C : " + r2);

        // Test 3: nearest ambulance to emergency at D
        Map<String, String> ambulances = new HashMap<>();
        ambulances.put("AMB-01", "Base1");
        ambulances.put("AMB-02", "Base2");

        Object[] nearest = city.findNearestAmbulance(ambulances, "D");
        System.out.println("Nearest ambulance to D: " + nearest[0]
                + " | " + nearest[1]);
    }
}
