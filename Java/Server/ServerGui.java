import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

class ServerGui extends JFrame {
    //String Constants
    private final static String START_MESSAGE = "Start the server (1245)";

    // Components
    JButton startServer, closeServer;
    JTextField portInput;
    private JTextArea outputArea;

    // Server
    private Server server;

    ServerGui(Server server) {
        setupLayout();
        setupFunctionality();
        setupFrame();
        this.server = server;
    }

    private void setupLayout() {
        // Center panel setup
        JPanel centerPanel = new JPanel();
        outputArea = new JTextArea(START_MESSAGE, 15, 30);
        outputArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret)outputArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        centerPanel.add(scrollPane);
        add(centerPanel, BorderLayout.CENTER);

        // South panel setup
        JPanel southPanel = new JPanel();
        portInput = new JTextField(6);
        portInput.setText("1245");
        southPanel.add(portInput);
        startServer = new JButton("Start Server");
        southPanel.add(startServer);
        closeServer = new JButton("Close Server");
        closeServer.setVisible(false);
        southPanel.add(closeServer);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void setupFrame() {
        setSize(400, 400);
        setTitle("Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        setResizable(false);
    }

    private void setupFunctionality() {
        startServer.addActionListener(e -> {
            try {
                server.setPort(Integer.parseInt(portInput.getText()));
                server.startServer();
                startServer.setVisible(false);
                portInput.setVisible(false);
                closeServer.setVisible(true);
            } catch(Exception ex) {
                outputArea.setText("Error: Unable to start the server");
                startServer.setVisible(true);
                portInput.setVisible(true);
                closeServer.setVisible(false);
            }
        });

        closeServer.addActionListener(e -> {
            try {
                server.serverSocket.close();
            } catch (Exception ignored) {}
            outputArea.setText(START_MESSAGE);
            startServer.setVisible(true);
            portInput.setVisible(true);
            closeServer.setVisible(false);
        });
    }

    synchronized void setOutput(String s) {
        outputArea.setText(s);
    }

    synchronized void appendOutput(String s) {
        outputArea.append("\n" + s);
    }
}
