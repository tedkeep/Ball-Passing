import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    ServerHandler server;

    private LinkedBlockingQueue<String> messages;
    private int id, ball, storedId;
    private ArrayList<Integer> clientIds;

    private ClientGui gui;

    public Client() {
        messages = new LinkedBlockingQueue<>();
        clientIds = new ArrayList<>();
        id = 0;
        ball = 0;
        storedId = 0;

        Thread messageHandling = new Thread(this::messageHandler);
        messageHandling.start();

        gui = new ClientGui(this);
    }

    private void messageHandler() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String[] message = messages.take().split(":");
                switch (message[0]) {
                    case "id":
                        if(storedId == 0) id = Integer.parseInt(message[1]);
                        else {
                            server.write("id:" + storedId);
                            id = storedId;
                            storedId = 0;
                        }
                        break;
                    case "ball":
                        ball = Integer.parseInt(message[1]);
                        if (ball == id) {
                            gui.setComponentVisibility(true, gui.getClientSelect(), gui.getPassBall());
                        } else {
                            gui.setComponentVisibility(false, gui.getClientSelect(), gui.getPassBall());
                        }
                        if (!clientIds.isEmpty()) {
                            gameOutput();
                        }
                        break;
                    case "clients":
                        clientIds.clear();
                        String[] strClientIds = message[1].split(" ");
                        for (String id :
                                strClientIds) {
                            if (!id.equals("")) {
                                clientIds.add(Integer.parseInt(id));
                            }
                        }
                        gui.getClients().removeAllElements();
                        gui.getClients().addAll(clientIds);
                        gameOutput();
                        break;
                    case "server":
                        if (message[1].equals("0")) server.closeConnection();
                        else {
                            if (ball == id) {
                                restartServer();
                            }
                            storedId = id;
                            while(true) {
                                try {
                                    connectToServer(1245);
                                    break;
                                } catch (Exception ignored) {}
                            }
                        }
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void connectToServer(int port) throws Exception {
        Socket socket = new Socket("0.0.0.0", port);
        server = new ServerHandler(socket);
    }

    private void restartServer() {
        try
        {
            ProcessBuilder pb = new ProcessBuilder("javac", "Server.java");
            pb.directory(new File("src"));
            pb.start().waitFor();


            pb = new ProcessBuilder("java", "Server", String.valueOf(ball), gui.getPortInput().getText());
            pb.directory(new File("src"));
            pb.start();
        }

        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    class ServerHandler {
        BufferedReader in;
        PrintWriter out;
        Socket socket;
        private boolean safeClose;

        ServerHandler(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            safeClose = false;

            Thread read = new Thread(this::readIncomingMessages);
            read.start();
        }

        void readIncomingMessages() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String message = in.readLine();
                    messages.put(message);
                } catch (Exception e) {
                    if(safeClose) {
                        gui.getOutputArea().setText("Connection to the server was ended.");
                        gui.setComponentVisibility(true, gui.getConnectToServer(), gui.getPortInput());
                        gui.setComponentVisibility(false, gui.getLeaveGame(), gui.getClientSelect(), gui.getPassBall());
                    } else {
                        try {
                            messages.put("server:1");
                        } catch (Exception ignored) {}
                    }
                    Thread.currentThread().interrupt();
                }
            }
        }

        void write(String message) {
            try {
                out.println(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void closeConnection() {
            try {
                safeClose = true;
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    private void gameOutput() {
        gui.getOutputArea().setText("==============================");
        gui.getOutputArea().append("\nYour ID: " + id);
        gui.getOutputArea().append("\nConnected clients: ");
        for (Integer id :
                clientIds) {
            if (id == ball) {
                gui.getOutputArea().append("\n    Client " + id + " <-- Has the ball!");
            } else {
                gui.getOutputArea().append("\n    Client " + id);
            }
        }

        gui.getOutputArea().append("\n==============================");
        if (ball == id) {
            gui.getOutputArea().append("\nYou have the ball!");
        } else {
            gui.getOutputArea().append("\nAnother player has the ball! \nWait for the ball to be passed to you.");
        }
    }


    public static void main(String[] args) {
        new Client();
    }
}


