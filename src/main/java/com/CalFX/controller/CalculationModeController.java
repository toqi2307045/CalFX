package com.CalFX.controller;

import com.CalFX.Navigator;
import com.CalFX.calculator.AngleMode;
import com.CalFX.calculator.PlaceholderEvaluator;
import com.CalFX.calculator.ResultFormatter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Provides the working forms for the non-basic calculation modes. */
public class CalculationModeController {
    private final Navigator navigator;
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final Map<String, ComboBox<String>> choices = new LinkedHashMap<>();
    private final Map<String, Node> rows = new LinkedHashMap<>();
    private final PlaceholderEvaluator evaluator = new PlaceholderEvaluator();
    private VBox polynomialCoefficients;

    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label resultLabel;
    @FXML private GridPane formGrid;

    public CalculationModeController(Navigator navigator) { this.navigator = navigator; }

    @FXML
    private void initialize() {
        String mode = navigator.getRequestedCalculationMode();
        titleLabel.setText(mode == null ? "Calculation" : mode);
        formGrid.setHgap(12);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(8, 0, 8, 0));
        switch (mode) {
            case "Matrix" -> setupMatrix();
            case "Complex numbers" -> setupComplex();
            case "Polynomial" -> setupPolynomial();
            case "Functions solver" -> setupSolver();
            case "Differentiation and integration" -> setupCalculus();
            case "Permutations and combinations" -> setupCombinatorics();
            case "Scientific constants and unit conversion" -> setupConstants();
            default -> descriptionLabel.setText("Select a calculation mode from the previous screen.");
        }
    }

    @FXML private void onBack() { navigator.showCalculationMenu(); }

    @FXML
    private void onCalculate() {
        try {
            String mode = navigator.getRequestedCalculationMode();
            String result = switch (mode) {
                case "Matrix" -> calculateMatrix();
                case "Complex numbers" -> calculateComplex();
                case "Polynomial" -> calculatePolynomial();
                case "Functions solver" -> calculateRoot();
                case "Differentiation and integration" -> calculateCalculus();
                case "Permutations and combinations" -> calculateCombinatorics();
                case "Scientific constants and unit conversion" -> calculateConstantOrConvert();
                default -> throw new IllegalArgumentException("Choose a calculation mode first.");
            };
            resultLabel.setText(result);
            resultLabel.getStyleClass().remove("calculation-error");
        } catch (IllegalArgumentException | ArithmeticException ex) {
            resultLabel.setText(ex.getMessage());
            if (!resultLabel.getStyleClass().contains("calculation-error")) {
                resultLabel.getStyleClass().add("calculation-error");
            }
        }
    }

    private void setupMatrix() {
        descriptionLabel.setText("Choose the size of each matrix, then enter a value in every cell. Addition and subtraction need matching sizes; multiplication needs A columns to match B rows.");
        ComboBox<String> operation = addChoice("operation", "Operation", List.of("Add", "Subtract", "Multiply", "Determinant", "Inverse"));
        addMatrixInput("A");
        addMatrixInput("B");
        operation.valueProperty().addListener((obs, oldValue, value) -> {
            boolean needsB = !"Determinant".equals(value) && !"Inverse".equals(value);
            setRowVisible("matrixBBox", needsB);
            setRowVisible("matrixBEntries", needsB);
            setRowVisible("matrixBRows", needsB);
            setRowVisible("matrixBColumns", needsB);
        });
        setRowVisible("matrixBBox", true);
    }

    private void addMatrixInput(String name) {
        String rowsId = "matrix" + name + "Rows";
        String columnsId = "matrix" + name + "Columns";
        ComboBox<String> rowCount = addChoice(rowsId, "Matrix " + name + " rows", dimensions());
        ComboBox<String> columnCount = addChoice(columnsId, "Matrix " + name + " columns", dimensions());
        rowCount.setValue("2");
        columnCount.setValue("2");
        GridPane cells = new GridPane();
        cells.setHgap(8);
        cells.setVgap(8);
        cells.getStyleClass().add("matrix-input-grid");
        VBox box = new VBox(6, new Label("Matrix " + name), cells);
        box.getStyleClass().add("matrix-input-section");
        int row = formGrid.getRowCount();
        Label entriesLabel = new Label("Matrix " + name + " entries");
        formGrid.add(entriesLabel, 0, row);
        formGrid.add(box, 1, row);
        rows.put("matrix" + name + "Box", box);
        rows.put("matrix" + name + "Entries", entriesLabel);
        Runnable update = () -> rebuildMatrixGrid(name, cells, Integer.parseInt(rowCount.getValue()), Integer.parseInt(columnCount.getValue()));
        rowCount.valueProperty().addListener((obs, oldValue, value) -> update.run());
        columnCount.valueProperty().addListener((obs, oldValue, value) -> update.run());
        update.run();
    }

    private static List<String> dimensions() {
        return java.util.stream.IntStream.rangeClosed(1, 8).mapToObj(String::valueOf).toList();
    }

    private void rebuildMatrixGrid(String name, GridPane grid, int rowCount, int columnCount) {
        grid.getChildren().clear();
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                String id = "matrix" + name + "_" + row + "_" + column;
                TextField cell = new TextField("0");
                cell.setPromptText("r" + (row + 1) + " c" + (column + 1));
                cell.setPrefColumnCount(5);
                cell.getStyleClass().add("mode-field");
                fields.put(id, cell);
                grid.add(cell, column, row);
            }
        }
    }

    private void setupComplex() {
        descriptionLabel.setText("Enter the real and imaginary parts of A and B, then choose an operation. Magnitude only needs A.");
        ComboBox<String> operation = addChoice("operation", "Operation", List.of("Add", "Subtract", "Multiply", "Divide", "Magnitude of A"));
        addField("a", "Real part a", "3");
        addField("b", "Imaginary part b", "2");
        addField("c", "Real part c", "1");
        addField("d", "Imaginary part d", "-4");
        operation.valueProperty().addListener((obs, oldValue, value) -> {
            setRowVisible("c", !"Magnitude of A".equals(value));
            setRowVisible("d", !"Magnitude of A".equals(value));
        });
    }

    private void setupPolynomial() {
        descriptionLabel.setText("Choose the polynomial degree and enter one coefficient for each power from highest to constant. Real roots are supported through degree two.");
        ComboBox<String> operation = addChoice("operation", "Operation", List.of("Evaluate", "Real roots"));
        ComboBox<String> degree = addChoice("degree", "Degree", java.util.stream.IntStream.rangeClosed(1, 10).mapToObj(String::valueOf).toList());
        degree.setValue("2");
        polynomialCoefficients = new VBox(6);
        polynomialCoefficients.getStyleClass().add("coefficient-list");
        int row = formGrid.getRowCount();
        formGrid.add(new Label("Coefficients"), 0, row);
        formGrid.add(polynomialCoefficients, 1, row);
        rows.put("coefficients", polynomialCoefficients);
        degree.valueProperty().addListener((obs, oldValue, value) -> rebuildCoefficients(Integer.parseInt(value)));
        rebuildCoefficients(Integer.parseInt(degree.getValue()));
        addField("x", "x value", "2");
        operation.valueProperty().addListener((obs, oldValue, value) -> setRowVisible("x", "Evaluate".equals(value)));
        setRowVisible("x", true);
    }

    private void rebuildCoefficients(int degree) {
        Map<String, String> previousValues = new LinkedHashMap<>();
        for (Map.Entry<String, TextField> entry : fields.entrySet()) {
            if (entry.getKey().startsWith("coefficient")) previousValues.put(entry.getKey(), entry.getValue().getText());
        }
        polynomialCoefficients.getChildren().clear();
        for (int power = degree; power >= 0; power--) {
            String id = "coefficient" + power;
            TextField coefficient = new TextField(previousValues.getOrDefault(id, power == degree ? "1" : "0"));
            fields.put(id, coefficient);
            coefficient.setPromptText("coefficient of x^" + power);
            if (!coefficient.getStyleClass().contains("mode-field")) coefficient.getStyleClass().add("mode-field");
            polynomialCoefficients.getChildren().add(new HBox(8, new Label("x^" + power), coefficient));
        }
    }

    private void setupSolver() {
        descriptionLabel.setText("Find a real root of f(x) in the interval. Use x as the variable and enter an interval whose endpoints bracket a root.");
        addField("function", "Function f(x)", "x^2 - 4");
        addField("lower", "Lower bound", "0");
        addField("upper", "Upper bound", "5");
    }

    private void setupCalculus() {
        descriptionLabel.setText("Use x in the function. Derivatives use a centered difference; definite integrals use Simpson's rule.");
        ComboBox<String> operation = addChoice("operation", "Operation", List.of("Derivative at x", "Definite integral"));
        addField("function", "Function f(x)", "sin(x)");
        addField("lower", "x value", "0");
        addField("upper", "Upper bound", "3.14159265359");
        operation.valueProperty().addListener((obs, oldValue, value) -> {
            boolean integral = "Definite integral".equals(value);
            setRowVisible("upper", integral);
            ((Label) rows.get("lower")).setText(integral ? "Lower bound" : "x value");
        });
        setRowVisible("upper", false);
    }

    private void setupCombinatorics() {
        descriptionLabel.setText("Calculate arrangements nPr and selections nCr for non-negative integers with 0 ≤ r ≤ n.");
        addChoice("operation", "Operation", List.of("Permutation (nPr)", "Combination (nCr)"));
        addField("n", "n", "10");
        addField("r", "r", "3");
    }

    private void setupConstants() {
        descriptionLabel.setText("Look up common scientific constants or convert a value between units in the selected category.");
        ComboBox<String> category = addChoice("category", "Mode", List.of("Constants", "Length", "Mass", "Time", "Temperature"));
        addChoice("constant", "Constant", List.of("π", "e", "Speed of light c", "Gravitational constant G", "Standard gravity g", "Avogadro constant Nₐ", "Boltzmann constant kB", "Planck constant h"));
        addField("value", "Value to convert", "1");
        ComboBox<String> from = addChoice("from", "From", unitsFor("Length"));
        ComboBox<String> to = addChoice("to", "To", unitsFor("Length"));
        category.valueProperty().addListener((obs, oldValue, newValue) -> {
            boolean constantMode = "Constants".equals(newValue);
            setRowVisible("constant", constantMode);
            setRowVisible("value", !constantMode);
            setRowVisible("from", !constantMode);
            setRowVisible("to", !constantMode);
            from.setItems(FXCollections.observableArrayList(unitsFor(newValue)));
            to.setItems(FXCollections.observableArrayList(unitsFor(newValue)));
            if (!constantMode) {
                from.getSelectionModel().selectFirst();
                to.getSelectionModel().select(1);
            }
        });
        category.setValue("Constants");
        setRowVisible("constant", true);
        setRowVisible("value", false);
        setRowVisible("from", false);
        setRowVisible("to", false);
    }

    private String calculateMatrix() {
        double[][] a = readMatrix("A");
        String operation = choice("operation");
        return switch (operation) {
            case "Determinant" -> format(determinant(a));
            case "Inverse" -> formatMatrix(inverse(a));
            case "Add", "Subtract" -> {
                double[][] b = readMatrix("B");
                requireSameShape(a, b);
                double[][] result = new double[a.length][a[0].length];
                double sign = operation.equals("Add") ? 1 : -1;
                for (int i = 0; i < a.length; i++) for (int j = 0; j < a[0].length; j++) result[i][j] = a[i][j] + sign * b[i][j];
                yield formatMatrix(result);
            }
            case "Multiply" -> {
                double[][] b = readMatrix("B");
                if (a[0].length != b.length) throw new IllegalArgumentException("Matrix A columns must match Matrix B rows.");
                double[][] result = new double[a.length][b[0].length];
                for (int i = 0; i < a.length; i++) for (int j = 0; j < b[0].length; j++)
                    for (int k = 0; k < b.length; k++) result[i][j] += a[i][k] * b[k][j];
                yield formatMatrix(result);
            }
            default -> throw new IllegalArgumentException("Select a matrix operation.");
        };
    }

    private String calculateComplex() {
        double a = value("a"), b = value("b");
        String operation = choice("operation");
        if (operation.equals("Magnitude of A")) return "|A| = " + format(Math.hypot(a, b));
        double c = value("c"), d = value("d");
        double real;
        double imaginary;
        switch (operation) {
            case "Add" -> { real = a + c; imaginary = b + d; }
            case "Subtract" -> { real = a - c; imaginary = b - d; }
            case "Multiply" -> { real = a * c - b * d; imaginary = a * d + b * c; }
            case "Divide" -> {
                double denominator = c * c + d * d;
                if (denominator == 0) throw new IllegalArgumentException("Cannot divide by zero complex number.");
                real = (a * c + b * d) / denominator;
                imaginary = (b * c - a * d) / denominator;
            }
            default -> throw new IllegalArgumentException("Select a complex operation.");
        }
        return format(real) + (imaginary < 0 ? " - " + format(-imaginary) + "i" : " + " + format(imaginary) + "i");
    }

    private String calculatePolynomial() {
        int selectedDegree = Integer.parseInt(choice("degree"));
        double[] coefficients = new double[selectedDegree + 1];
        for (int i = 0; i <= selectedDegree; i++) coefficients[i] = value("coefficient" + (selectedDegree - i));
        while (coefficients.length > 1 && coefficients[0] == 0) coefficients = java.util.Arrays.copyOfRange(coefficients, 1, coefficients.length);
        if (choice("operation").equals("Evaluate")) {
            double x = value("x"), result = 0;
            for (double coefficient : coefficients) result = result * x + coefficient;
            return "P(" + format(x) + ") = " + format(result);
        }
        int effectiveDegree = coefficients.length - 1;
        if (effectiveDegree == 1) return "Real root: " + format(-coefficients[1] / coefficients[0]);
        if (effectiveDegree != 2) throw new IllegalArgumentException("Real roots are available for linear and quadratic polynomials.");
        double discriminant = coefficients[1] * coefficients[1] - 4 * coefficients[0] * coefficients[2];
        if (discriminant < 0) return "No real roots (discriminant < 0).";
        double root = Math.sqrt(discriminant);
        return "Real roots: " + format((-coefficients[1] - root) / (2 * coefficients[0]) )
                + (root == 0 ? "" : ", " + format((-coefficients[1] + root) / (2 * coefficients[0])));
    }

    private String calculateRoot() {
        String function = text("function");
        double lower = value("lower"), upper = value("upper");
        if (!(lower < upper)) throw new IllegalArgumentException("Lower bound must be less than upper bound.");
        double previousX = lower;
        double previousY = eval(function, previousX);
        if (previousY == 0) return "Root: " + format(previousX);
        double left = Double.NaN, right = Double.NaN;
        for (int i = 1; i <= 2000; i++) {
            double x = lower + (upper - lower) * i / 2000.0;
            double y = eval(function, x);
            if (y == 0 || Math.signum(previousY) != Math.signum(y)) { left = previousX; right = x; break; }
            previousX = x; previousY = y;
        }
        if (Double.isNaN(left)) throw new IllegalArgumentException("No sign change found in the interval; try a wider interval.");
        double fLeft = eval(function, left);
        for (int i = 0; i < 100; i++) {
            double middle = (left + right) / 2;
            double fMiddle = eval(function, middle);
            if (Math.abs(fMiddle) < 1e-12 || right - left < 1e-12) return "Root: " + format(middle);
            if (Math.signum(fMiddle) == Math.signum(fLeft)) { left = middle; fLeft = fMiddle; } else right = middle;
        }
        return "Root: " + format((left + right) / 2);
    }

    private String calculateCalculus() {
        String function = text("function");
        double lower = value("lower");
        if (choice("operation").equals("Derivative at x")) {
            double h = 1e-5 * Math.max(1, Math.abs(lower));
            return "f'(" + format(lower) + ") ≈ " + format((eval(function, lower + h) - eval(function, lower - h)) / (2 * h));
        }
        double upper = value("upper");
        if (lower == upper) return "Integral = 0";
        int intervals = 10000;
        double h = (upper - lower) / intervals;
        double sum = eval(function, lower) + eval(function, upper);
        for (int i = 1; i < intervals; i++) sum += (i % 2 == 0 ? 2 : 4) * eval(function, lower + i * h);
        return "Definite integral ≈ " + format(sum * h / 3);
    }

    private String calculateCombinatorics() {
        int n = integer("n"), r = integer("r");
        if (n < 0 || r < 0 || r > n || n > 10000) throw new IllegalArgumentException("Use integers with 0 ≤ r ≤ n ≤ 10000.");
        if (choice("operation").startsWith("Permutation")) return "nPr = " + factorial(n).divide(factorial(n - r));
        int choose = Math.min(r, n - r);
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= choose; i++) result = result.multiply(BigInteger.valueOf(n - choose + i)).divide(BigInteger.valueOf(i));
        return "nCr = " + result;
    }

    private String calculateConstantOrConvert() {
        if (choice("category").equals("Constants")) {
            double constant = switch (choice("constant")) {
                case "π" -> Math.PI;
                case "e" -> Math.E;
                case "Speed of light c" -> 299_792_458;
                case "Gravitational constant G" -> 6.67430e-11;
                case "Standard gravity g" -> 9.80665;
                case "Avogadro constant Nₐ" -> 6.02214076e23;
                case "Boltzmann constant kB" -> 1.380649e-23;
                case "Planck constant h" -> 6.62607015e-34;
                default -> throw new IllegalArgumentException("Select a constant.");
            };
            return choice("constant") + " = " + format(constant);
        }
        double input = value("value");
        String category = choice("category"), from = choice("from"), to = choice("to");
        if (from.equals(to)) return format(input) + " " + to;
        double converted;
        if (category.equals("Temperature")) {
            double celsius = switch (from) { case "°F" -> (input - 32) * 5 / 9; case "K" -> input - 273.15; default -> input; };
            converted = switch (to) { case "°F" -> celsius * 9 / 5 + 32; case "K" -> celsius + 273.15; default -> celsius; };
        } else {
            Map<String, Double> factors = unitFactors(category);
            converted = input * factors.get(from) / factors.get(to);
        }
        return format(input) + " " + from + " = " + format(converted) + " " + to;
    }

    private ComboBox<String> addChoice(String id, String label, List<String> values) {
        int row = formGrid.getRowCount();
        Label rowLabel = new Label(label);
        formGrid.add(rowLabel, 0, row);
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(values));
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add("mode-field");
        combo.getSelectionModel().selectFirst();
        formGrid.add(combo, 1, row);
        rows.put(id, rowLabel);
        rows.put(id + "Control", combo);
        choices.put(id, combo);
        return combo;
    }

    private TextField addField(String id, String label, String prompt) {
        int row = formGrid.getRowCount();
        Label rowLabel = new Label(label);
        formGrid.add(rowLabel, 0, row);
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        field.getStyleClass().add("mode-field");
        formGrid.add(field, 1, row);
        rows.put(id, rowLabel);
        rows.put(id + "Control", field);
        fields.put(id, field);
        return field;
    }

    private void setRowVisible(String id, boolean visible) {
        Node node = rows.get(id);
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
        Node control = rows.get(id + "Control");
        if (control != null) { control.setVisible(visible); control.setManaged(visible); }
    }

    private double[][] readMatrix(String name) {
        int rowCount = Integer.parseInt(choice("matrix" + name + "Rows"));
        int columnCount = Integer.parseInt(choice("matrix" + name + "Columns"));
        double[][] matrix = new double[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                matrix[row][column] = value("matrix" + name + "_" + row + "_" + column);
            }
        }
        return matrix;
    }

    private String text(String id) {
        String value = fields.get(id).getText().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Enter a value for " + inputName(id) + ".");
        return value;
    }

    private double value(String id) {
        try { return Double.parseDouble(text(id)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a valid number for " + inputName(id) + "."); }
    }

    private static String inputName(String id) {
        if (id.startsWith("matrix") && id.matches("matrix[AB]_\\d+_\\d+")) {
            String[] parts = id.substring(8).split("_");
            return "Matrix " + id.charAt(6) + ", row " + (Integer.parseInt(parts[0]) + 1) + ", column " + (Integer.parseInt(parts[1]) + 1);
        }
        if (id.startsWith("coefficient")) return "coefficient of x^" + id.substring("coefficient".length());
        return id;
    }

    private int integer(String id) {
        try { return Integer.parseInt(text(id)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(id + " must be an integer."); }
    }

    private String choice(String id) {
        String value = choices.get(id).getValue();
        if (value == null) throw new IllegalArgumentException("Select an option for " + id + ".");
        return value;
    }

    private double eval(String expression, double x) {
        try { return evaluator.evaluate(expression, x, AngleMode.RADIANS); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Cannot evaluate f(x): " + ex.getMessage()); }
    }

    private static void requireSquare(double[][] matrix) {
        if (matrix.length == 0 || matrix.length != matrix[0].length) throw new IllegalArgumentException("Matrix must be square.");
    }

    private static void requireSameShape(double[][] a, double[][] b) {
        if (a.length != b.length || a[0].length != b[0].length) throw new IllegalArgumentException("Matrices must have the same dimensions.");
    }

    private static double determinant(double[][] source) {
        requireSquare(source);
        double[][] a = copy(source);
        double determinant = 1;
        for (int i = 0; i < a.length; i++) {
            int pivot = i;
            for (int row = i + 1; row < a.length; row++) if (Math.abs(a[row][i]) > Math.abs(a[pivot][i])) pivot = row;
            if (Math.abs(a[pivot][i]) < 1e-14) return 0;
            if (pivot != i) { double[] temp = a[i]; a[i] = a[pivot]; a[pivot] = temp; determinant = -determinant; }
            determinant *= a[i][i];
            for (int row = i + 1; row < a.length; row++) {
                double factor = a[row][i] / a[i][i];
                for (int col = i + 1; col < a.length; col++) a[row][col] -= factor * a[i][col];
            }
        }
        return determinant;
    }

    private static double[][] inverse(double[][] source) {
        requireSquare(source);
        int n = source.length;
        double[][] a = new double[n][2 * n];
        for (int i = 0; i < n; i++) { System.arraycopy(source[i], 0, a[i], 0, n); a[i][n + i] = 1; }
        for (int i = 0; i < n; i++) {
            int pivot = i;
            for (int row = i + 1; row < n; row++) if (Math.abs(a[row][i]) > Math.abs(a[pivot][i])) pivot = row;
            if (Math.abs(a[pivot][i]) < 1e-14) throw new IllegalArgumentException("This matrix is singular and has no inverse.");
            double[] temp = a[i]; a[i] = a[pivot]; a[pivot] = temp;
            double divisor = a[i][i];
            for (int col = 0; col < 2 * n; col++) a[i][col] /= divisor;
            for (int row = 0; row < n; row++) if (row != i) {
                double factor = a[row][i];
                for (int col = 0; col < 2 * n; col++) a[row][col] -= factor * a[i][col];
            }
        }
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(a[i], n, inverse[i], 0, n);
        return inverse;
    }

    private static double[][] copy(double[][] matrix) {
        double[][] copy = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) copy[i] = matrix[i].clone();
        return copy;
    }

    private static String formatMatrix(double[][] matrix) {
        StringBuilder output = new StringBuilder();
        for (double[] row : matrix) {
            if (!output.isEmpty()) output.append('\n');
            output.append("[ ");
            for (int j = 0; j < row.length; j++) { if (j > 0) output.append(",  "); output.append(format(row[j])); }
            output.append(" ]");
        }
        return output.toString();
    }

    private static String format(double value) {
        if (!Double.isFinite(value)) throw new ArithmeticException("Result is not finite.");
        return ResultFormatter.format(value);
    }

    private static BigInteger factorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) result = result.multiply(BigInteger.valueOf(i));
        return result;
    }

    private static List<String> unitsFor(String category) {
        return switch (category) {
            case "Length" -> List.of("m", "km", "cm", "mm", "in", "ft", "mi");
            case "Mass" -> List.of("kg", "g", "mg", "lb", "oz");
            case "Time" -> List.of("s", "min", "h", "day");
            case "Temperature" -> List.of("°C", "°F", "K");
            default -> List.of("m", "km", "cm", "mm", "in", "ft", "mi");
        };
    }

    private static Map<String, Double> unitFactors(String category) {
        Map<String, Double> factors = new LinkedHashMap<>();
        switch (category) {
            case "Length" -> { factors.put("m", 1.0); factors.put("km", 1000.0); factors.put("cm", 0.01); factors.put("mm", 0.001); factors.put("in", 0.0254); factors.put("ft", 0.3048); factors.put("mi", 1609.344); }
            case "Mass" -> { factors.put("kg", 1.0); factors.put("g", 0.001); factors.put("mg", 1e-6); factors.put("lb", 0.45359237); factors.put("oz", 0.028349523125); }
            case "Time" -> { factors.put("s", 1.0); factors.put("min", 60.0); factors.put("h", 3600.0); factors.put("day", 86400.0); }
            default -> throw new IllegalArgumentException("Select a supported conversion category.");
        }
        return factors;
    }
}
