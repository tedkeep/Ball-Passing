using System;
using System.Collections;
using System.Collections.Concurrent;
using System.IO;
using System.Net.Sockets;
using System.Text;
using System.Threading;

namespace Assignment
{
    public class Server
    {
        private static ArrayList clientsList;
        private static BlockingCollection<String> receivedClientMessages;
        private TcpListener serverSocket;
        private int port;
        private static int clientIdCount;
        private static int clientWithTheBall;

        public Server()
        {
            clientsList = new ArrayList();
            receivedClientMessages = new BlockingCollection<string>();
            clientIdCount = clientWithTheBall = 0;
            port = 1245;
            startServer();
        }

        public Server(int ball, int port)
        {
            clientsList = new ArrayList();
            receivedClientMessages = new BlockingCollection<string>();
            clientIdCount = 0;
            this.port = port;
            clientWithTheBall = ball;
            startServer();
        }

        private void startServer()
        {
            Thread acceptConnections = new Thread(connectionAcceptHandler);
            acceptConnections.Start();

            Thread messageHandling = new Thread(incomingMessagesHandler);
            messageHandling.Start();

            Console.WriteLine("Server started on port " + port);
        }

        private void incomingMessagesHandler()
        {
            while (Thread.CurrentThread.IsAlive)
            {
                try
                {
                    String[] message = receivedClientMessages.Take().Split(':');

                    switch (message[0])
                    {
                        case "pass":
                            Boolean clientExists = false;
                            foreach (ClientHandler client in clientsList)
                            {
                                if (client.clientId == int.Parse(message[1]))
                                {
                                    clientExists = true;
                                    Console.WriteLine("Client " + clientWithTheBall + " passed the ball to client " + message[1]);
                                    clientWithTheBall = int.Parse(message[1]);
                                    sendToAllClients("ball:" + clientWithTheBall);
                                }
                            }
                            if (!clientExists)
                            {
                                Console.WriteLine("Client not found. Ball not passed");
                            }
                            break;
                    }
                }
                catch (Exception) { }
            }
        }

        private void connectionAcceptHandler()
        {
            try
            {
                serverSocket = new TcpListener(System.Net.IPAddress.Parse("0.0.0.0"), port);
                serverSocket.Start();
                while (true)
                {
                    TcpClient clientSocket = serverSocket.AcceptTcpClient();
                    clientIdCount++;
                    ClientHandler newClient = new ClientHandler(clientSocket, clientIdCount);
                    clientsList.Add(newClient);
                    newClient.setupClient();
                    displayConnectedClients();
                }
            }
            catch (Exception)
            {
                sendToAllClients("server:0");
                clientsList = new ArrayList();
                receivedClientMessages = new BlockingCollection<string>();
                clientIdCount = 1;
                clientWithTheBall = 0;
            }
        }

        private static void displayConnectedClients()
        {
            Console.WriteLine("Current clients connected");
            if (clientsList.Count == 0)
            {
                Console.WriteLine("  None");
            }
            else
            {
                foreach (ClientHandler client in clientsList)
                {
                    Console.WriteLine("  Client " + client.clientId);
                }
            }
        }

        class ClientHandler
        {
            public StreamWriter outMessage;
            StreamReader inMessage;
            public int clientId;

            public ClientHandler(TcpClient clientSocket, int clientId)
            {
                this.clientId = clientId;
                Stream ns = clientSocket.GetStream();
                outMessage = new StreamWriter(ns);
                inMessage = new StreamReader(outMessage.BaseStream);
                outMessage.AutoFlush = true;
                Thread read = new Thread(incomingClientMessageHandler);
                read.Start();

                Console.WriteLine("Client " + clientId + " connected to the server");
            }

            public void setupClient()
            {
                sendToClient("id:" + clientId);
                if (clientWithTheBall == 0)
                {
                    clientWithTheBall = clientId;
                    Console.WriteLine("Client " + clientId + " has been handed the ball by the server");
                }
                sendToClient("ball:" + clientWithTheBall);
                sendClientIdsToAllClients();
            }

            public void sendToClient(String message)
            {
                try
                {
                    outMessage.WriteLine(message);
                    outMessage.Flush();
                }
                catch (Exception) { }
            }

            private void incomingClientMessageHandler()
            {
                while (Thread.CurrentThread.IsAlive)
                {
                    try
                    {
                        String message = inMessage.ReadLine();
                        if (message.Contains("id"))
                        {
                            String[] id = message.Split(':');
                            Console.WriteLine("Client " + clientId + " updated its ID to " + int.Parse(id[1]));
                            clientId = int.Parse(id[1]);
                            if (clientId > clientIdCount)
                            {
                                clientIdCount = clientId;
                            }
                            sendClientIdsToAllClients();
                            displayConnectedClients();
                        }
                        else receivedClientMessages.Add(message);
                        
                    }
                    catch (Exception e)
                    {
                        // client has disconnected
                        // remove client handler from list
                        clientsList.Remove(this);
                        // show an up to date list of clients
                        Console.WriteLine("Client " + clientId + " has disconnected.");
                        displayConnectedClients();
                        // check if client list is empty. if so set client with ball back to default
                        if (clientsList.Count == 0)
                        {
                            clientWithTheBall = 0;
                            clientIdCount = 0;
                        }
                        else
                        {
                            if (clientWithTheBall == clientId)
                            {
                                try
                                {
                                    ClientHandler client = (ClientHandler)clientsList[0];
                                    receivedClientMessages.Add("pass:" + client.clientId);
                                }
                                catch (Exception) { }
                            }
                           
                            sendClientIdsToAllClients();
                        }
                        
                        break;
                    }
                }
            }
        }

        public static void sendClientIdsToAllClients()
        {
            StringBuilder clientIds = new StringBuilder("clients:");
            foreach (ClientHandler client in clientsList)
            {
                clientIds.Append(" ").Append(client.clientId);
            }
            sendToAllClients(clientIds.ToString());
        }

        private static void sendToAllClients(String message)
        {
            foreach (ClientHandler client in clientsList)
            {
                client.sendToClient(message);
            }
        }

        static void Main(string[] args)
        {
            if (args.Length > 0)
            {
                new Server(int.Parse(args[0]), int.Parse(args[1]));
            }
            else
                new Server();
        }
    }
}

