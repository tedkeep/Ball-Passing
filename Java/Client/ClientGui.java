import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

class ClientGui extends JFrame {
    private JButton passBall, connectToServer, leaveGame;
    private JTextArea outputArea;
    private JComboBox<Integer> clientSelect;
    private JTextField portInput;
    private DefaultComboBoxModel<Integer> clients;
    private Client client;

    ClientGui(Client client) {
        setupLayout();
        setupFunctionality();
        setupFrame();
        this.client = client;
    }

    private void setupFunctionality() {
        passBall.addActionListener(e -> {
            if(clientSelect.getSelectedIndex() != -1) {
                client.server.write("pass:" + clientSelect.getItemAt(clientSelect.getSelectedIndex()));
            }
        });

        connectToServer.addActionListener(e -> {
            String port = portInput.getText();

            try {
                client.connectToServer(Integer.parseInt(port));
                setComponentVisibility(false, connectToServer, portInput);
                setComponentVisibility(true, leaveGame);
            } catch(Exception ex) {
                outputArea.setText("Error: Unable to connect to the server");
            }
        });

        leaveGame.addActionListener(e -> {
            client.server.closeConnection();
            setComponentVisibility(true, connectToServer, portInput);
            setComponentVisibility(false, leaveGame);
        });
    }

    private void setupFrame() {
        setSize(400, 400);
        setTitle("Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        setResizable(false);
    }

    private void setupLayout() {
        JPanel northPanel = new JPanel();
        portInput = new JTextField(6);
        portInput.setText("1245");
        northPanel.add(portInput);
        connectToServer = new JButton("Connect");
        northPanel.add(connectToServer);
        leaveGame = new JButton("Leave the game");
        leaveGame.setVisible(false);
        northPanel.add(leaveGame);
        add(northPanel, BorderLayout.NORTH);


        JPanel centerPanel = new JPanel();
        outputArea = new JTextArea("Enter a port to join a game (1245)", 15, 30);
        outputArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret)outputArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        centerPanel.add(scrollPane);
        add(centerPanel, BorderLayout.CENTER);


        JPanel southPanel = new JPanel();
        clients = new DefaultComboBoxModel<>();
        clientSelect = new JComboBox<>(clients);
        clientSelect.setVisible(false);
        southPanel.add(clientSelect);
        passBall = new JButton("Pass the ball");
        passBall.setVisible(false);
        southPanel.add(passBall);
        southPanel.setSize(getWidth(), 150);
        add(southPanel, BorderLayout.SOUTH);
    }

    void setComponentVisibility(boolean state, JComponent ...components) {
        for (JComponent component:
             components) {
            component.setVisible(state);
        }
    }

    JButton getPassBall() {
        return passBall;
    }

    JButton getLeaveGame() {
        return leaveGame;
    }

    JButton getConnectToServer() {
        return connectToServer;
    }

    JComboBox<Integer> getClientSelect() {
        return clientSelect;
    }

    JTextField getPortInput() {
        return portInput;
    }

    JTextArea getOutputArea() {
        return outputArea;
    }

    DefaultComboBoxModel<Integer> getClients() {
        return clients;
    }
}
