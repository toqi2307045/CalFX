package com.CalFX.graph;

import com.CalFX.calculator.ResultFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.DoubleUnaryOperator;

/**
 * The graph window: grid, axes, tick labels and one plotted function.
 * Mouse wheel zooms around the cursor, dragging pans the view.
 * It knows nothing about parsing; it only receives a DoubleUnaryOperator to plot.
 *
 * Threading:
 * - The function is evaluated on a background thread (one worker, one Task at a time).
 *   A newer request cancels the older Task, so a slow function can never freeze the window.
 * - The JavaFX thread only draws. Samples are kept in math coordinates, so panning and zooming
 *   repaint instantly from the last finished samples while new samples are being computed.
 * - The DoubleUnaryOperator is therefore called from a worker thread and must be thread-safe.
 */
public class GraphPane extends Pane {

    private static final double DEFAULT_SCALE = 50;      // pixels per unit
    private static final double MIN_SCALE = 0.001;
    private static final double MAX_SCALE = 100_000;
    private static final double MIN_MAJOR_SPACING = 70;  // pixels between labelled grid lines
    private static final double CORNER_ARC = 30;

    private static final Color BACKGROUND = Color.WHITE;
    private static final Color MINOR_GRID = Color.web("#EEF3FD");
    private static final Color MAJOR_GRID = Color.web("#D5E0F5");
    private static final Color AXIS = Color.web("#23386B");
    private static final Color LABEL = Color.web("#6B7A99");
    private static final Color CURVE = Color.web("#1A56DB");

    /** Finished result of one sampling run, in math coordinates. Never modified after creation. */
    private record Samples(double[] x, double[] y, double scale) {
    }

    private final Canvas canvas = new Canvas();
    private final Rectangle clip = new Rectangle();
    private final StringProperty coordinatesText = new SimpleStringProperty("x = 0.000    y = 0.000");

    // one daemon worker thread: it never keeps the application alive after the window is closed
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "graph-sampler");
        thread.setDaemon(true);
        return thread;
    });

    // the fields below are only touched on the JavaFX thread
    private Task<Samples> samplingTask;     // newest task; results of older tasks are ignored
    private Samples samples;                // last finished result, null when nothing is plotted
    private DoubleUnaryOperator function;
    private double centerX;                 // math coordinates at the middle of the pane
    private double centerY;
    private double scale = DEFAULT_SCALE;

    private double dragStartX;
    private double dragStartY;
    private double dragCenterX;
    private double dragCenterY;

    public GraphPane() {
        getChildren().add(canvas);
        clip.setArcWidth(CORNER_ARC);
        clip.setArcHeight(CORNER_ARC);
        setClip(clip);
        setPrefSize(700, 420);
        setCursor(Cursor.CROSSHAIR);

        setOnScroll(this::onScroll);
        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnMouseMoved(this::updateCoordinates);

        // when the screen is replaced (Navigator swaps the root) the pane leaves the scene: stop the worker
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                stopWorker();
            }
        });
    }

    // ---------------------------------------------------------------- public API

    /**
     * Plots the given function, or clears the curve when null.
     * The function is called on a background thread, so it must be thread-safe.
     */
    public void setFunction(DoubleUnaryOperator function) {
        this.function = function;
        samples = null;
        redraw();
        requestSampling();
    }

    public void resetView() {
        centerX = 0;
        centerY = 0;
        scale = DEFAULT_SCALE;
        viewChanged();
    }

    /** factor greater than 1 zooms in, smaller than 1 zooms out. */
    public void zoomBy(double factor) {
        zoomAt(getWidth() / 2, getHeight() / 2, factor);
    }

    public StringProperty coordinatesTextProperty() {
        return coordinatesText;
    }

    // ---------------------------------------------------------------- layout

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        clip.setWidth(getWidth());
        clip.setHeight(getHeight());
        viewChanged();
    }

    // ---------------------------------------------------------------- mouse

    private void onScroll(ScrollEvent event) {
        if (event.getDeltaY() != 0) {
            zoomAt(event.getX(), event.getY(), Math.pow(1.0025, event.getDeltaY()));
        }
    }

    private void onMousePressed(MouseEvent event) {
        dragStartX = event.getX();
        dragStartY = event.getY();
        dragCenterX = centerX;
        dragCenterY = centerY;
    }

    private void onMouseDragged(MouseEvent event) {
        centerX = dragCenterX - (event.getX() - dragStartX) / scale;
        centerY = dragCenterY + (event.getY() - dragStartY) / scale;
        updateCoordinates(event);
        viewChanged();
    }

    private void updateCoordinates(MouseEvent event) {
        coordinatesText.set(String.format(Locale.ROOT, "x = %.3f    y = %.3f",
                toMathX(event.getX()), toMathY(event.getY())));
    }

    private void zoomAt(double pixelX, double pixelY, double factor) {
        double mathX = toMathX(pixelX);
        double mathY = toMathY(pixelY);
        scale = clamp(scale * factor, MIN_SCALE, MAX_SCALE);
        // keep the point under the cursor where it was
        centerX = mathX - (pixelX - getWidth() / 2) / scale;
        centerY = mathY + (pixelY - getHeight() / 2) / scale;
        viewChanged();
    }

    // ---------------------------------------------------------------- coordinates

    private double toScreenX(double x) {
        return getWidth() / 2 + (x - centerX) * scale;
    }

    private double toScreenY(double y) {
        return getHeight() / 2 - (y - centerY) * scale;
    }

    private double toMathX(double pixelX) {
        return centerX + (pixelX - getWidth() / 2) / scale;
    }

    private double toMathY(double pixelY) {
        return centerY - (pixelY - getHeight() / 2) / scale;
    }

    // ---------------------------------------------------------------- background sampling

    /** Called after every change of the view: repaint at once, and resample only if needed. */
    private void viewChanged() {
        redraw();
        if (function != null && !samplesCoverView()) {
            requestSampling();
        }
    }

    /** True when the finished samples have the current zoom and reach across the visible area. */
    private boolean samplesCoverView() {
        if (samples == null || samples.scale() != scale) {
            return false;
        }
        double[] xs = samples.x();
        return xs[0] <= toMathX(0) && xs[xs.length - 1] >= toMathX(getWidth());
    }

    /**
     * Starts computing the y values on the worker thread. Must be called on the JavaFX thread.
     * One extra view width is sampled on each side, so short pans need no new calculation.
     */
    private void requestSampling() {
        if (samplingTask != null) {
            samplingTask.cancel();
            samplingTask = null;
        }
        if (function == null || getWidth() <= 0 || worker.isShutdown()) {
            return;
        }

        // copy everything the worker needs now: the worker must never read the pane's fields
        final DoubleUnaryOperator f = function;
        final double from = toMathX(-getWidth());
        final double step = 1 / scale;
        final double sampleScale = scale;
        final int count = (int) Math.ceil(3 * getWidth()) + 1;

        Task<Samples> task = new Task<>() {
            @Override
            protected Samples call() {
                double[] xs = new double[count];
                double[] ys = new double[count];
                for (int i = 0; i < count; i++) {
                    if (isCancelled()) {
                        return null;
                    }
                    xs[i] = from + i * step;
                    ys[i] = f.applyAsDouble(xs[i]);
                }
                return new Samples(xs, ys, sampleScale);
            }
        };
        // both handlers run on the JavaFX thread
        task.setOnSucceeded(event -> {
            if (task == samplingTask) {          // ignore results that were replaced meanwhile
                samples = task.getValue();
                redraw();
            }
        });
        task.setOnFailed(event -> {
            if (task == samplingTask) {
                samples = null;
                redraw();
                task.getException().printStackTrace();
            }
        });

        samplingTask = task;
        worker.execute(task);
    }

    private void stopWorker() {
        if (samplingTask != null) {
            samplingTask.cancel();
            samplingTask = null;
        }
        worker.shutdownNow();
    }

    // ---------------------------------------------------------------- drawing (JavaFX thread only)

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);

        double step = niceStep(MIN_MAJOR_SPACING / scale);
        drawGridLines(gc, w, h, step / 5, MINOR_GRID);
        drawGridLines(gc, w, h, step, MAJOR_GRID);
        drawAxes(gc, w, h);
        drawLabels(gc, w, h, step);
        drawFunction(gc, w, h);
    }

    private void drawGridLines(GraphicsContext gc, double w, double h, double spacing, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(1);

        long firstX = (long) Math.ceil(toMathX(0) / spacing);
        for (long i = firstX; i * spacing <= toMathX(w); i++) {
            double px = Math.round(toScreenX(i * spacing)) + 0.5;
            gc.strokeLine(px, 0, px, h);
        }
        long firstY = (long) Math.ceil(toMathY(h) / spacing);
        for (long i = firstY; i * spacing <= toMathY(0); i++) {
            double py = Math.round(toScreenY(i * spacing)) + 0.5;
            gc.strokeLine(0, py, w, py);
        }
    }

    private void drawAxes(GraphicsContext gc, double w, double h) {
        gc.setStroke(AXIS);
        gc.setLineWidth(1.6);
        double originX = toScreenX(0);
        double originY = toScreenY(0);
        if (originX >= 0 && originX <= w) {
            gc.strokeLine(originX, 0, originX, h);
        }
        if (originY >= 0 && originY <= h) {
            gc.strokeLine(0, originY, w, originY);
        }
    }

    private void drawLabels(GraphicsContext gc, double w, double h, double step) {
        gc.setFill(LABEL);
        gc.setFont(Font.font(12));

        // labels stay next to the axes but never leave the visible area
        double labelY = clamp(toScreenY(0) + 16, 14, h - 6);
        double labelX = clamp(toScreenX(0) - 6, 44, w - 4);

        gc.setTextAlign(TextAlignment.CENTER);
        long firstX = (long) Math.ceil(toMathX(0) / step);
        for (long i = firstX; i * step <= toMathX(w); i++) {
            if (i != 0) {
                gc.fillText(ResultFormatter.format(i * step), toScreenX(i * step), labelY);
            }
        }

        gc.setTextAlign(TextAlignment.RIGHT);
        long firstY = (long) Math.ceil(toMathY(h) / step);
        for (long i = firstY; i * step <= toMathY(0); i++) {
            if (i != 0) {
                gc.fillText(ResultFormatter.format(i * step), labelX, toScreenY(i * step) + 4);
            }
        }
        gc.fillText("0", labelX, labelY);
    }

    /** Draws the finished samples; no function is evaluated here. */
    private void drawFunction(GraphicsContext gc, double w, double h) {
        if (samples == null) {
            return;
        }
        double[] xs = samples.x();
        double[] ys = samples.y();

        gc.setStroke(CURVE);
        gc.setLineWidth(2.5);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.beginPath();

        boolean penDown = false;
        double previousY = 0;
        for (int i = 0; i < xs.length; i++) {
            double px = toScreenX(xs[i]);
            if (px < -20) {
                penDown = false;                      // left of the visible area
                continue;
            }
            if (px > w + 20) {
                break;                                // right of the visible area
            }
            double py = toScreenY(ys[i]);
            boolean drawable = Double.isFinite(py) && Math.abs(py) < 1e6;
            if (!drawable) {
                penDown = false;                      // gap: undefined or far off screen
                continue;
            }
            // a huge jump between neighbouring samples is an asymptote (tan x, 1/x): do not join it
            if (penDown && Math.abs(py - previousY) < 2 * h) {
                gc.lineTo(px, py);
            } else {
                gc.moveTo(px, py);
            }
            penDown = true;
            previousY = py;
        }
        gc.stroke();
    }

    // ---------------------------------------------------------------- helpers

    /** Picks 1, 2 or 5 times a power of ten that is at least rawStep. */
    private static double niceStep(double rawStep) {
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double fraction = rawStep / magnitude;
        double nice = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10;
        return nice * magnitude;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
