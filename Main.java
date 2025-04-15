import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;

public class Main {
    private static DrawingCanvas canvas = new DrawingCanvas();
    private static JFrame frame = new JFrame();
    private static JScrollPane scrollPane = new JScrollPane(canvas);

    public static void main(String[] args) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLayout(new BorderLayout());
        //frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Get the screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setSize(screenSize.width, screenSize.height);
        canvas.setPreferredSize(new Dimension((int)(screenSize.width * 0.9), (int)(screenSize.height * 0.8)));

        // Add shortcuts for undo/redo
        InputMap iMap = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap aMap = canvas.getActionMap();

        iMap.put(KeyStroke.getKeyStroke("control Z"), "undoAction");
        aMap.put("undoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.undo();
            }
        });

        iMap.put(KeyStroke.getKeyStroke("control Y"), "redoAction");
        aMap.put("redoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.redo();
            }
        });

        // Create the menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem loadMenuItem = new JMenuItem("Load");
        fileMenu.add(loadMenuItem);
        loadMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.loadImage();
            }
        });

        JMenuItem saveMenuItem = new JMenuItem("Save As");
        fileMenu.add(saveMenuItem);
        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.saveImg();
            }
        });

        frame.setJMenuBar(menuBar);

        addCtrlPanel();

        // making the frame visible
        frame.setVisible(true);
    }

    static void addCtrlPanel() {
        JPanel ctrlPanel = new JPanel();

        frame.getContentPane().add(ctrlPanel, BorderLayout.NORTH);

        // Undo/Redo buttons
        JButton undoButton = new JButton("Undo");
        JButton redoButton = new JButton("Redo");

        undoButton.setToolTipText("Ctrl+Z");
        redoButton.setToolTipText("Ctrl+Y");

        undoButton.addActionListener(e -> canvas.undo());
        redoButton.addActionListener(e -> canvas.redo());

        ctrlPanel.add(undoButton);
        ctrlPanel.add(redoButton);

        JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        zoomSlider.setBorder(BorderFactory.createTitledBorder("Zoom"));
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.addChangeListener(e -> {
            int sliderValue = zoomSlider.getValue();
            canvas.setZoomFactor(sliderValue / 100.0);
            canvas.repaint();
        });
        ctrlPanel.add(zoomSlider);

        JSlider brushSizeSlider = new JSlider(JSlider.HORIZONTAL, 2, 18, 5);
        brushSizeSlider.setBorder(BorderFactory.createTitledBorder("Brush Size"));
        brushSizeSlider.setMajorTickSpacing(8);
        brushSizeSlider.setMinorTickSpacing(1);
        brushSizeSlider.setPaintTicks(true);
        brushSizeSlider.setPaintLabels(true);
        brushSizeSlider.addChangeListener(e -> {
            int sliderValue = brushSizeSlider.getValue();
            canvas.setBrushSize(sliderValue);
            canvas.repaint();
        });
        ctrlPanel.add(brushSizeSlider);

        // Button to change the color
        JButton colorButton = new JButton("Choose color");
        colorButton.addActionListener(e -> canvas.changeColor());
        ctrlPanel.add(colorButton);

        // Buttons to draw shapes
        ButtonGroup shapeButtonsGroup = new ButtonGroup();

        // Toggle button for each shape type.
        JToggleButton brushButton = new JToggleButton("Brush");
        JToggleButton lineButton = new JToggleButton("Line");
        JToggleButton rectButton = new JToggleButton("Rectangle");
        JToggleButton ovalButton = new JToggleButton("Oval");
        JToggleButton triButton = new JToggleButton("Triangle");

        brushButton.addActionListener(e -> canvas.setShapeType(DrawingCanvas.shapeType.NONE));
        lineButton.addActionListener(e -> canvas.setShapeType(DrawingCanvas.shapeType.LINE));
        rectButton.addActionListener(e -> canvas.setShapeType(DrawingCanvas.shapeType.RECTANGLE));
        ovalButton.addActionListener(e -> canvas.setShapeType(DrawingCanvas.shapeType.OVAL));
        triButton.addActionListener(e -> canvas.setShapeType(DrawingCanvas.shapeType.TRIANGLE));

        shapeButtonsGroup.add(brushButton);
        shapeButtonsGroup.add(lineButton);
        shapeButtonsGroup.add(rectButton);
        shapeButtonsGroup.add(ovalButton);
        shapeButtonsGroup.add(triButton);

        brushButton.setSelected(true);

        ctrlPanel.add(brushButton);
        ctrlPanel.add(lineButton);
        ctrlPanel.add(rectButton);
        ctrlPanel.add(ovalButton);
        ctrlPanel.add(triButton);
    }
}

class DrawingCanvas extends JPanel {
    // Stores the drawing in memory to save later
    private BufferedImage img;

    // Helps in drawing smoothly
    private Graphics2D G2D;

    // A flag to indicate that the user is drawing
    private boolean isDrawing = false;

    public enum shapeType {NONE, LINE, RECTANGLE, OVAL, TRIANGLE }

    private shapeType currentShapeType = shapeType.NONE;

    // Mouse coordinates
    private int startX, startY;
    private int currentX, currentY;  // updated during dragging

    private int brushSize = 5;
    private double zoomFactor = 1.0;

    private BufferedImage[] imageHistory = new BufferedImage[25];
    private int historyPointer = 0; // Points at the last changed image in imageHistory
    private int changesMade = 0; // The number of changes the user made
    private int changesReverted = 0; // The number of times the user hit undo

    // The brush color
    Color drawingColor = Color.BLACK;

    public DrawingCanvas() {
        MouseAdapter mouseAdapt = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startX = (int) (e.getX() / zoomFactor); // X-Coordinate adjusted to zoom
                startY = (int) (e.getY() / zoomFactor); // Y-Coordinate adjusted to zoom
                currentX = startX;
                currentY = startY;

                if (currentShapeType == shapeType.NONE) {
                    G2D.fillOval(currentX - (brushSize / 2), currentY - (brushSize / 2), brushSize, brushSize);
                }

                isDrawing = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawing && G2D != null) {
                    // Update the current coordinates one final time
                    currentX = (int) (e.getX() / zoomFactor); // X-Coordinate adjusted to zoom
                    currentY = (int) (e.getY() / zoomFactor); // Y-Coordinate adjusted to zoom

                    if (currentShapeType == shapeType.NONE) {
                        G2D.fillOval(currentX - (brushSize / 2), currentY - (brushSize / 2), brushSize, brushSize);
                    }

                    drawShape(G2D, startX, startY, currentX, currentY, currentShapeType);
                    isDrawing = false;

                    // Record change
                    BufferedImage imgCopy = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
                    Graphics2D g = imgCopy.createGraphics();
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                    historyPointer++;
                    imageHistory[historyPointer % 25] = imgCopy; // % 25 to loop over the array and reuse the memory
                    changesMade++;

                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentShapeType == shapeType.NONE && isDrawing) {
                    G2D.drawLine(currentX, currentY, (int) (e.getX() / zoomFactor), (int) (e.getY() / zoomFactor));
                }
                currentX = (int) (e.getX() / zoomFactor); // X-Coordinate adjusted to zoom
                currentY = (int) (e.getY() / zoomFactor); // Y-Coordinate adjusted to zoom
                repaint();
            }
        };

        addMouseListener(mouseAdapt);
        addMouseMotionListener(mouseAdapt);
    }

    private void drawShape(Graphics2D g, int x1, int y1, int x2, int y2, shapeType type) {
        g.setColor(drawingColor);
        // Calculate common parameters for rectangular shapes
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);

        switch (type) {
            case LINE:
                g.drawLine(x1, y1, x2, y2);
                break;
            case RECTANGLE:
                g.drawRect(x, y, width, height);
                break;
            case OVAL:
                g.drawOval(x, y, width, height);
                break;
            case TRIANGLE:
                // Define a simple triangle: top-center, bottom-left, and bottom-right.
                int[] xPoints = { x + width / 2, x, x + width };
                int[] yPoints = { y, y + height, y + height };
                g.drawPolygon(xPoints, yPoints, 3);
                break;
            default:
                break;
        }
    }

    public void setZoomFactor(double z) {
        zoomFactor = z;
    }

    public void setShapeType(shapeType s) {
        currentShapeType = s;
    }

    public void setBrushSize(int size) {
        brushSize = size;
        if (G2D != null) {
            G2D.setStroke(new BasicStroke(brushSize));
        }
    }

    public void changeColor() {
        Color selectedColor = JColorChooser.showDialog(null, "Choose a Drawing Color", drawingColor);
        if (selectedColor != null) {
            drawingColor = selectedColor; // Update the drawing color
            G2D.setColor(drawingColor);
        }
    }

    public int getChangesMade() {
        return changesMade;
    }

    public int getChangesReverted() {
        return changesReverted;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (img == null) {
            img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            G2D = img.createGraphics();

            G2D.setColor(Color.WHITE);
            G2D.fillRect(0, 0, getWidth(), getHeight());

            // Make drawing smoother
            G2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            G2D.setColor(drawingColor);
            G2D.setStroke(new BasicStroke(brushSize));

            // Add it to the imageHistory
            BufferedImage imgCopy = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
            Graphics2D g2d = imgCopy.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();
            imageHistory[historyPointer % 25] = imgCopy;
        }
        else if (img.getWidth() != getWidth() || img.getHeight() != getHeight()) {
            // Create a new image with the new dimensions
            BufferedImage newImg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D newG2D = newImg.createGraphics();

            // Make white background
            newG2D.setColor(Color.WHITE);
            newG2D.fillRect(0, 0, getWidth(), getHeight());

            // Copy the old image into the new one
            newG2D.drawImage(img, 0, 0, null);

            // Dispose the temporary graphics context
            newG2D.dispose();

            // Replace the old image with the new one
            img = newImg;
            G2D = img.createGraphics();

            // Make drawing smoother
            G2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            G2D.setColor(drawingColor);
            G2D.setStroke(new BasicStroke(brushSize));
        }

        // Create a copy of g so we don't permanently alter it
        Graphics2D g2d = (Graphics2D) g.create();

        // Apply zoom scaling to the entire drawing
        g2d.scale(zoomFactor, zoomFactor);

        // Draw the permanent image
        g2d.drawImage(img, 0, 0, null);

        // If we're currently drawing, overlay a preview shape without committing it
        if (isDrawing) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(drawingColor);
            g2.setStroke(new BasicStroke(brushSize));
            g2.scale(zoomFactor, zoomFactor);
            drawShape(g2, startX, startY, currentX, currentY, currentShapeType);
            g2.dispose();
        }
        g2d.dispose();
    }

    public void saveImg() {
        JFileChooser dialog = new JFileChooser(FileSystemView.getFileSystemView());
        // Remove the default filter
        dialog.setAcceptAllFileFilterUsed(false);

        // Allow the user to only choose png or jpg
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG (*.png)", "png");
        FileNameExtensionFilter jpgFilter = new FileNameExtensionFilter("JPEG (*.jpg, *.jpeg)", "jpg", "jpeg");
        dialog.addChoosableFileFilter(pngFilter);
        dialog.addChoosableFileFilter(jpgFilter);

        // Only save the image if the user approved
        int userSelection = dialog.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String fileName = dialog.getSelectedFile().getAbsolutePath();
            // Determine which filter is active:
            FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) dialog.getFileFilter();
            String[] extensions = selectedFilter.getExtensions(); // An array of the extensions chosen

            // Check if file name ends with any allowed extension
            boolean isValidExt = false;
            for (String ext : extensions) {
                if (fileName.endsWith("." + ext.toLowerCase())) {
                    isValidExt = true;
                    break;
                }
            }

            // Add the first extension if necessary
            if (!isValidExt) {
                fileName = fileName.concat("." + extensions[0]);
            }

            // Remove the alpha channel in case of jpg saving
            if (extensions[0] == "jpg") {
                BufferedImage rgbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = rgbImg.createGraphics();
                g2d.drawImage(img, 0, 0, null);
                g2d.dispose();
                img = rgbImg;
            }

            // Save the image
            try {
                ImageIO.write(img, extensions[0], new File(fileName));
                System.out.println("Drawing saved successfully!");
                System.out.println(fileName);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Error saving the drawing.");
            }
        }
    }

    public void loadImage() {
        JFileChooser dialog = new JFileChooser(FileSystemView.getFileSystemView());
        // Remove the default filter
        dialog.setAcceptAllFileFilterUsed(false);

        // Allow the user to only choose png or jpg
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("PNG, JPG, JPEG", "png", "jpg", "jpeg");
        dialog.addChoosableFileFilter(imageFilter);

        int userSelection = dialog.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String imagePath = dialog.getSelectedFile().getAbsolutePath();
            File file = new File(imagePath);
            if (file.exists() && file.isFile()) {
                System.out.println("The file exists!");
            } else {
                System.out.println("The file does not exist.");
                return;
            }

            // Scale the image and show it on screen
            try {
                BufferedImage loadedImage = ImageIO.read(new File(imagePath));

                // Get the panel's current dimensions
                int canvasWidth = getWidth();
                int canvasHeight = getHeight();

                // Calculate scale factors for width and height
                double scaleX = (double) canvasWidth / loadedImage.getWidth();
                double scaleY = (double) canvasHeight / loadedImage.getHeight();

                // Choose the smaller scale to preserve image ratio
                double scale = Math.min(scaleX, scaleY);

                int newWidth = (int) (loadedImage.getWidth() * scale);
                int newHeight = (int) (loadedImage.getHeight() * scale);

                // Create a new image with the new dimensions
                BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = scaledImage.createGraphics();

                // Use bilinear interpolation for smoother scaling
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(loadedImage, 0, 0, newWidth, newHeight, null);
                g2d.dispose();

                // Replace your canvas image with the scaled image
                img = scaledImage;
                // Update the Graphics2D
                G2D = img.createGraphics();
                G2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                repaint();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void undo() {
        if (changesMade > 0) {
            historyPointer--;
            BufferedImage prevImg = imageHistory[historyPointer % 25];
            Graphics2D g = img.createGraphics();
            g.drawImage(prevImg, 0, 0, null);
            g.dispose();
            changesMade--;
            changesReverted++;
            repaint();
        }
    }

    public void redo() {
        if (changesReverted > 0) {
            historyPointer++;
            BufferedImage prevImg = imageHistory[historyPointer % 25];
            Graphics2D g = img.createGraphics();
            g.drawImage(prevImg, 0, 0, null);
            g.dispose();
            changesMade++;
            changesReverted--;
            repaint();
        }
    }
}
