import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private ArrayList<ClientHandler> clientsList;
    private LinkedBlockingQueue<String> receivedClientMessages;
    private int clientIdCount, clientWithTheBall;

    ServerSocket serverSocket;
    private int port;

    private ServerGui gui;

    Server() {
        clientsList = new ArrayList<>();
        receivedClientMessages = new LinkedBlockingQueue<>();
        clientIdCount = clientWithTheBall = port = 0;
        gui = new ServerGui(this);
    }

    Server(int ball, int port) {
        clientsList = new ArrayList<>();
        receivedClientMessages = new LinkedBlockingQueue<>();
        clientWithTheBall = ball;
        clientIdCount = 0;
        this.port = port;
        gui = new ServerGui(this);
        startServer();
        gui.startServer.setVisible(false);
        gui.portInput.setVisible(false);
        gui.closeServer.setVisible(true);
    }

    void startServer() {
        Thread acceptConnections = new Thread(this::connectionAcceptHandler);
        acceptConnections.start();

        Thread messageHandling = new Thread(this::incomingMessagesHandler);
        messageHandling.start();

        gui.setOutput("Server started on port " + port);
    }

    private void incomingMessagesHandler() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                String[] message = receivedClientMessages.take().split(":");

                switch (message[0]) {
                    case "pass":
                        // check player is still in the game
                        boolean clientExists = false;
                        for (ClientHandler client :
                                clientsList) {
                            if (client.clientId == Integer.parseInt(message[1])) {
                                clientExists = true;
                                gui.appendOutput("Client " + clientWithTheBall + " passed the ball to client " + message[1]);
                                clientWithTheBall = Integer.parseInt(message[1]);
                                sendToAllClients("ball:" + clientWithTheBall);
                            }
                        }
                        if (!clientExists) {
                            System.out.println("client not found. Ball not passed");
                        }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectionAcceptHandler() {
        try {
            serverSocket = new ServerSocket(port);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                clientIdCount++;
                ClientHandler newClient = new ClientHandler(clientSocket, clientIdCount);
                clientsList.add(newClient);
                newClient.setupClient();
                displayConnectedClients();
            }
        } catch (IOException e) {
            // tell all clients the server has been closed
            sendToAllClients("server:0");
            clientsList = new ArrayList<>();
            receivedClientMessages = new LinkedBlockingQueue<>();
            clientIdCount = 1;
            clientWithTheBall = 0;
        }
    }

    private void displayConnectedClients() {
        gui.appendOutput("Current clients connected");
        if(clientsList.isEmpty()) {
            gui.appendOutput("  None");
        } else {
            for (ClientHandler client :
                    clientsList) {
                gui.appendOutput("  Client " + client.clientId);
            }
        }
    }

    void setPort(int parseInt) {
        port = parseInt;
    }

    private class ClientHandler {
        BufferedReader in;
        PrintWriter out;
        Socket clientSocket;
        int clientId;

        ClientHandler(Socket clientSocket, int clientId) throws IOException {
            this.clientSocket = clientSocket;
            this.clientId = clientId;
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            Thread read = new Thread(this::IncomingClientMessagesHandler);
            read.start();

            gui.appendOutput("Client " + clientId + " connected to the server");
        }

        private void setupClient() {
            sendToClient("id:" + clientId);
            if(clientWithTheBall == 0) {
                clientWithTheBall = clientId;
                gui.appendOutput("Client " + clientId + " has been handed the ball by the server");
            }
            sendToClient("ball:"+clientWithTheBall);
            sendClientIdsToAllClients();
        }

        void sendToClient(String message) {
            try {
                out.println(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void IncomingClientMessagesHandler() {
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    String message = in.readLine();
                    if(message.contains("id")) {
                        String[] id = message.split(":");
                        gui.appendOutput("Client " + clientId + " updated its ID to " + Integer.parseInt(id[1]));
                        clientId = Integer.parseInt(id[1]);
                        if(clientId > clientIdCount) {
                            clientIdCount = clientId;
                        }
                        sendClientIdsToAllClients();
                        displayConnectedClients();
                    }
                    else receivedClientMessages.put(message);
                } catch (Exception e) {
                    // client has disconnected
                    // remove client handler from list
                    clientsList.remove(this);
                    // show an up to date list of clients
                    gui.appendOutput("Client " + clientId + " has disconnected.");
                    displayConnectedClients();
                    // check if client list is empty. if so set client with ball back to default
                    if(clientsList.isEmpty()) {
                        clientWithTheBall = 0;
                        clientIdCount = 0;
                    } else {
                        if(clientWithTheBall == clientId) {
                            try {
                                receivedClientMessages.put("pass:" + clientsList.get(0).clientId);
                            } catch (InterruptedException ignored) {}
                        }
                        sendClientIdsToAllClients();
                    }
                    // interrupt the thread
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void sendClientIdsToAllClients() {
        StringBuilder clientIds = new StringBuilder("clients:");
        for (ClientHandler client:
                clientsList) {
            clientIds.append(" ").append(client.clientId);
        }
        sendToAllClients(clientIds.toString());
    }

    private void sendToAllClients(String message) {
        for (ClientHandler client:
                clientsList) {
            client.sendToClient(message);
        }
    }

    public static void main(String[] args) {
        if(args.length > 0) {
            new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } else
        new Server();
    }
}
