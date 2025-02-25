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
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DrawingCanvas can = new DrawingCanvas();

        frame.setLayout(new BorderLayout());
        //frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(can, BorderLayout.CENTER);

        // 400 width and 500 height
        frame.setSize(1080, 720);

        // Create the menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem newMenuItem = new JMenuItem("New");
        fileMenu.add(newMenuItem);

        JMenuItem saveMenuItem = new JMenuItem("Save");
        fileMenu.add(saveMenuItem);
        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                can.saveImg();
            }
        });

        frame.setJMenuBar(menuBar);

        JPanel ctrlPanel = new JPanel();
        JButton colorButton = new JButton("Choose color");

        frame.getContentPane().add(ctrlPanel, BorderLayout.NORTH);

        colorButton.addActionListener(e -> can.changeColor());
        ctrlPanel.add(colorButton);

        // making the frame visible
        frame.setVisible(true);
    }
}

class DrawingCanvas extends JPanel {
    // Stores the drawing in memory to save later
    private BufferedImage img;

    // Helps in drawing smoothly
    private Graphics2D G2D;

    // A flag to indicate that the user is drawing
    private boolean isDrawing = false;

    // Last mouse coordinates
    private int prevX, prevY;

    // The brush color
    Color drawingColor = Color.BLACK;

    public DrawingCanvas() {
        MouseAdapter mouseAdapt = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                prevX = e.getX();
                prevY = e.getY();
                isDrawing = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDrawing = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawing && G2D != null) {
                    int currentX = e.getX();
                    int currentY = e.getY();

                    G2D.drawLine(prevX, prevY, currentX, currentY);

                    prevX = currentX;
                    prevY = currentY;
                    repaint();
                }
            }
        };

        addMouseListener(mouseAdapt);
        addMouseMotionListener(mouseAdapt);
    }

    public void changeColor() {
        Color selectedColor = JColorChooser.showDialog(null, "Choose a Drawing Color", drawingColor);
        if (selectedColor != null) {
            drawingColor = selectedColor; // Update the drawing color
            G2D.setColor(drawingColor);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (img == null || img.getWidth() != getWidth() || img.getHeight() != getHeight()) {
            img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            G2D = img.createGraphics();

            G2D.setColor(Color.WHITE);
            G2D.fillRect(0, 0, getWidth(), getHeight());

            // Make drawing smoother
            G2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            G2D.setColor(drawingColor);
            G2D.setStroke(new BasicStroke(10));
        }

        g.drawImage(img, 0, 0, null);
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
}