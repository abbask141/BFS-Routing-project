// Import necessary JavaFX and Java utility classes
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.*;

// Main class that extends Application to launch a JavaFX GUI app
public class Main extends Application {
    // Store each node's visual representation (circle)
    private final Map<String, Circle> nodes = new HashMap<>();

    // Store graph connections: node -> list of its connected nodes
    private final Map<String, List<String>> graph = new HashMap<>();

    // Store position of each node on screen (x, y)
    private final Map<String, double[]> coordinates = new HashMap<>();

    // Store parent info for BFS to reconstruct the path
    private final Map<String, String> parent = new HashMap<>();

    // Default positions for first 6 nodes (A to F)
    private final double[][] defaultPositions = {
            {200, 300}, {300, 200}, {300, 400}, {400, 150}, {400, 450}, {500, 300}
    };

    private Group root; // Group to hold all visual elements

    // Dropdowns for GUI control
    private ComboBox<String> startSelector, endSelector, connectToSelector, removeSelector;

    private VBox controlPanel; // Layout for buttons and dropdowns

    private int nodeCount = 0; // To place new nodes in a pattern

    @Override
    public void start(Stage stage) {
        root = new Group(); // Initialize root group
        buildGraph();       // Create initial graph nodes and connections
        drawGraph();        // Visually draw them

        // Initialize dropdowns for selecting nodes
        startSelector = new ComboBox<>();
        endSelector = new ComboBox<>();
        connectToSelector = new ComboBox<>();
        removeSelector = new ComboBox<>();

        // Placeholder text for dropdowns
        startSelector.setPromptText("Start Node");
        endSelector.setPromptText("End Node");
        connectToSelector.setPromptText("Connect To Node");
        removeSelector.setPromptText("Select Node to Remove");

        updateDropdowns(); // Fill dropdowns with current node labels

        // Button to run BFS from selected start to end node
        Button runBFS = createStyledButton("Run BFS");
        runBFS.setOnAction(e -> {
            resetColors();
            String start = startSelector.getValue();
            String end = endSelector.getValue();
            if (start != null && end != null) {
                new Thread(() -> bfs(start, end)).start(); // Run BFS in new thread
            }
        });

        // Button to manually add an edge between node A and F
        Button addEdgeBtn = createStyledButton("Add Edge A-F");
        addEdgeBtn.setOnAction(e -> {
            if (graph.containsKey("A") && graph.containsKey("F") && !graph.get("A").contains("F")) {
                connect("A", "F");
                drawLineSafe("A", "F", Color.DARKGRAY);
            }
        });

        // Text field to type a new node label
        TextField nodeNameField = new TextField();
        nodeNameField.setPromptText("Node Label");
        nodeNameField.setStyle("-fx-background-color: #222; -fx-text-fill: #eee; -fx-border-color: #00ffcc;");

        // Button to add a new node and optionally connect it
        Button addNodeBtn = createStyledButton("Add Node");
        addNodeBtn.setOnAction(e -> {
            String label = nodeNameField.getText().toUpperCase();
            String connectTo = connectToSelector.getValue();
            if (!label.isEmpty() && !nodes.containsKey(label)) {
                double x = 150 + (nodeCount % 5) * 100;
                double y = 500;
                coordinates.put(label, new double[]{x, y});
                graph.put(label, new ArrayList<>());
                Circle circle = createNeonNode(x, y, label);
                nodes.put(label, circle);
                root.getChildren().add(circle);
                nodeCount++;
                updateDropdowns();
                if (connectTo != null && graph.containsKey(connectTo)) {
                    connect(label, connectTo);
                    drawLineSafe(label, connectTo, Color.DARKGRAY);
                }
            }
        });

        // Button to remove selected node from graph and GUI
        Button removeNodeBtn = createStyledButton("Remove Node");
        removeNodeBtn.setOnAction(e -> {
            String node = removeSelector.getValue();
            if (node != null && graph.containsKey(node)) {
                for (String neighbor : graph.get(node)) {
                    graph.get(neighbor).remove(node);
                }
                graph.remove(node);
                coordinates.remove(node);

                Platform.runLater(() -> {
                    root.getChildren().removeIf(n -> {
                        if (n instanceof Circle) {
                            Circle c = (Circle) n;
                            return c.getCenterX() == nodes.get(node).getCenterX() && c.getCenterY() == nodes.get(node).getCenterY();
                        }
                        if (n instanceof Text) {
                            return ((Text) n).getText().equals(node);
                        }
                        return false;
                    });
                    nodes.remove(node);
                });
                updateDropdowns();
            }
        });

        // Button to completely reset the graph to original 6 nodes
        Button resetBtn = createStyledButton("Reset Graph");
        resetBtn.setOnAction(e -> resetGraph());

        // Add all controls into a vertical layout box
        controlPanel = new VBox(10, startSelector, endSelector, runBFS, addEdgeBtn,
                nodeNameField, connectToSelector, addNodeBtn, removeSelector, removeNodeBtn, resetBtn);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setLayoutX(620);
        controlPanel.setLayoutY(20);
        root.getChildren().add(controlPanel);

        // Final scene setup
        Scene scene = new Scene(root, 900, 600, Color.web("#0d0d0d"));
        stage.setTitle("\uD83C\uDF0C BFS Routing Visualizer");
        stage.setScene(scene);
        stage.show();
    }

    // Add default nodes and edges in a clean pattern
    private void buildGraph() {
        String[] labels = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < labels.length; i++) {
            graph.put(labels[i], new ArrayList<>());
            coordinates.put(labels[i], defaultPositions[i]);
        }
        connect("A", "B");
        connect("A", "C");
        connect("C", "E");
        connect("E", "F");
        connect("F", "D");
        connect("B", "D");
    }

    // Draw nodes and edges visually
    private void drawGraph() {
        for (String label : coordinates.keySet()) {
            double[] pos = coordinates.get(label);
            Circle circle = createNeonNode(pos[0], pos[1], label);
            nodes.put(label, circle);
            root.getChildren().add(circle);
        }
        Set<String> drawn = new HashSet<>();
        for (String from : graph.keySet()) {
            for (String to : graph.get(from)) {
                String key1 = from + "-" + to;
                String key2 = to + "-" + from;
                if (!drawn.contains(key1) && !drawn.contains(key2)) {
                    drawLineSafe(from, to, Color.DARKGRAY);
                    drawn.add(key1);
                }
            }
        }
    }

    // Rebuild everything from scratch
    private void resetGraph() {
        Platform.runLater(() -> {
            root.getChildren().clear();
            nodes.clear();
            graph.clear();
            coordinates.clear();
            parent.clear();
            nodeCount = 0;
            buildGraph();
            drawGraph();
            root.getChildren().add(controlPanel);
            updateDropdowns();
        });
    }

    // Update dropdowns to reflect current nodes
    private void updateDropdowns() {
        Platform.runLater(() -> {
            List<String> labels = new ArrayList<>(graph.keySet());
            startSelector.getItems().setAll(labels);
            endSelector.getItems().setAll(labels);
            connectToSelector.getItems().setAll(labels);
            removeSelector.getItems().setAll(labels);
        });
    }

    // Create a neon glowing circle with label
    private Circle createNeonNode(double x, double y, String label) {
        Circle circle = new Circle(x, y, 20);
        circle.setFill(Color.web("#3c9aff"));
        circle.setStroke(Color.web("#00ffe1"));
        circle.setStrokeWidth(2);
        circle.setEffect(new DropShadow(18, Color.web("#00ffe1")));
        Text text = new Text(x - 5, y + 5, label);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Consolas", 14));
        Platform.runLater(() -> root.getChildren().add(text));
        return circle;
    }

    // Create consistent styled buttons
    private Button createStyledButton(String label) {
        Button button = new Button(label);
        button.setStyle("-fx-background-color: #222; -fx-text-fill: #ffffff; -fx-border-color: #00ffcc;");
        return button;
    }

    // Connect nodes bidirectionally
    private void connect(String from, String to) {
        graph.get(from).add(to);
        graph.get(to).add(from);
    }

    // Draw line between nodes in thread-safe way
    private void drawLineSafe(String from, String to, Color color) {
        double[] fromPos = coordinates.get(from);
        double[] toPos = coordinates.get(to);
        Platform.runLater(() -> {
            Line line = new Line(fromPos[0], fromPos[1], toPos[0], toPos[1]);
            line.setStroke(color);
            line.setStrokeWidth(2);
            root.getChildren().add(line);
        });
    }

    // Run BFS and visualize it
    private void bfs(String start, String end) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        parent.clear();
        queue.add(start);
        visited.add(start);
        highlight(start, Color.LIME);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(end)) {
                drawPath(start, end);
                return;
            }
            for (String neighbor : graph.get(current)) {
                if (!visited.contains(neighbor)) {
                    parent.put(neighbor, current);
                    visited.add(neighbor);
                    queue.add(neighbor);
                    highlight(neighbor, Color.ORANGE);
                    sleep(400);
                }
            }
        }
    }

    // Draw path from end to start using parent map
    private void drawPath(String start, String end) {
        String current = end;
        while (!current.equals(start)) {
            String prev = parent.get(current);
            drawLineSafe(prev, current, Color.web("#00ffcc"));
            current = prev;
        }
    }

    // Highlight a node with a specific color
    private void highlight(String nodeId, Color color) {
        Circle circle = nodes.get(nodeId);
        if (circle != null) {
            Platform.runLater(() -> circle.setFill(color));
        }
    }

    // Reset all node colors to default
    private void resetColors() {
        for (Circle c : nodes.values()) {
            Platform.runLater(() -> c.setFill(Color.web("#3c9aff")));
        }
    }

    // Pause the thread for visual delay
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    // Start the application
    public static void main(String[] args) {
        launch();
    }
}
