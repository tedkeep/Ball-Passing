using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.IO;
using System.Net.Sockets;
using System.Threading;

namespace AssignmentClient
{
    public class Client
    {
        ServerHandler server;

        public static BlockingCollection<String> receivedClientMessages;
        int id, ball, storedId;
        ArrayList clientIds;

        public Client()
        {
            receivedClientMessages = new BlockingCollection<String>();
            clientIds = new ArrayList();
            id = 0;
            ball = 0;
            storedId = 0;

            Thread messageHandling = new Thread(messageHandler);
            messageHandling.Start();

            Thread keyboardHandling = new Thread(keyboardHandler);
            keyboardHandling.Start();

            connectToServer(1245);
        }

        private void keyboardHandler()
        {
            while (true)
            {
                try
                {
                    String input = Console.ReadLine();
                    input = input.ToLower();
                    if (id == ball)
                    {
                        if (inputValidation(input))
                        {
                            String[] splitted = input.Split(":");

                            switch (splitted[0])
                            {
                                case "pass":
                                    if (!clientIds.Contains(int.Parse(splitted[1])))
                                    {
                                        Console.WriteLine("Client ID does not exist. Please enter a valid ID");
                                        break;
                                    }
                                    server.write(input);
                                    break;
                                default:

                                    break;
                            }
                        }
                        else
                        {
                            gameOutput();
                            Console.WriteLine("Not a valid input");
                            Console.WriteLine("use the following commands:");
                            Console.WriteLine("- pass:[id]");
                        }
                    }
                    else
                    {
                        Console.WriteLine("You dont have the ball, please wait for it to be passed to you");
                    }

                }
                catch (Exception)
                {
                }
            }
        }

        private void messageHandler()
        {
            while (Thread.CurrentThread.IsAlive)
            {
                try
                {
                    String[] message = receivedClientMessages.Take().Split(":");
                    switch (message[0])
                    {
                        case "id":
                            if (storedId == 0) id = int.Parse(message[1]);
                            else
                            {
                                server.write("id:" + storedId);
                                id = storedId;
                                storedId = 0;
                            }
                            break;
                        case "ball":
                            ball = int.Parse(message[1]);
                            if (clientIds.Count != 0)
                            {
                                gameOutput();
                            }
                            break;
                        case "clients":
                            clientIds.Clear();
                            String[] strClientIds = message[1].Split(" ");
                            foreach (String id in strClientIds)
                            {
                                if (!id.Equals(""))
                                {
                                    clientIds.Add(int.Parse(id));
                                }
                            }
                            gameOutput();
                            break;
                        case "server":
                            if (message[1].Equals("0")) server.closeConnection();
                            else
                            {
                                if (ball == id)
                                {
                                    restartServer();
                                }
                                storedId = id;
                                while (true)
                                {
                                    try
                                    {
                                        connectToServer(1245);
                                        break;
                                    }
                                    catch (Exception) { }
                                }
                            }
                            break;
                    }
                }
                catch (Exception)
                {
                    Thread.CurrentThread.Abort();
                }
            }
        }

        void restartServer()
        {
            try
            {
                using (Process compileServer = new Process())
                {
                    compileServer.StartInfo.UseShellExecute = true;
                    compileServer.StartInfo.FileName = "C:\\windows\\Microsoft.NET\\Framework\\v4.0.30319\\csc.exe";
                    compileServer.StartInfo.Arguments = "/t:exe /out:C#Server.exe \"C:\\Users\\tedgk\\OneDrive - University of Essex\\University\\Year 3\\ce303\\Assignmentc#\\Assignment\\Server_refactor.cs\"";
                    compileServer.StartInfo.CreateNoWindow = true;
                    compileServer.Start();
                    compileServer.WaitForExit();
                }

                using (Process restartServer = new Process())
                {
                    restartServer.StartInfo.UseShellExecute = true;
                    restartServer.StartInfo.CreateNoWindow = true;
                    restartServer.StartInfo.FileName = "C#Server.exe";
                    restartServer.StartInfo.Arguments = ball.ToString() + " 1245";
                    restartServer.Start();
                }

            }
            catch (Exception e)
            {
                Console.WriteLine(e.Message);
            }
        }

            Boolean inputValidation(String input)
            {
                if (input.Contains(":"))
                {
                    String[] inputSplit = input.Split(":");
                    if (!(inputSplit.Length > 2) && inputSplit[0].Equals("pass"))
                    {
                        try
                        {
                            int.Parse(inputSplit[1]);
                            return true;
                        }
                        catch (Exception)
                        {
                            return false;
                        }
                    }
                }
                return false;
            }

            void connectToServer(int port)
            {
                try
                {
                    TcpClient socket = new TcpClient("localhost", 1245);
                    server = new ServerHandler(socket);
                }
                catch (Exception)
                {
                    Console.WriteLine("Couldn't connect to server. Please start the server and then restart this program");
                    throw;
                }
            }

        class ServerHandler
        {
            StreamWriter outMessage;
            StreamReader inMessage;
            TcpClient socket;
            private Boolean safeClose;

            public ServerHandler(TcpClient socket)
            {
                safeClose = false;
                this.socket = socket;
                Stream ns = socket.GetStream();
                outMessage = new StreamWriter(ns);
                inMessage = new StreamReader(outMessage.BaseStream);
                outMessage.AutoFlush = true;
                Thread read = new Thread(readIncomingMessages);
                read.Start();
            }

            void readIncomingMessages()
            {
                while (Thread.CurrentThread.IsAlive)
                {
                    try
                    {
                        String message = inMessage.ReadLine();
                        receivedClientMessages.Add(message);
                    }
                    catch (Exception)
                    {
                        if(safeClose)
                        {
                            Console.WriteLine("Connection to the server was ended.");
                        } else
                        {
                            receivedClientMessages.Add("server:1");
                        }
                        
                        break;

                    }
                }
            }
            public void write(String message)
            {
                try
                {
                    outMessage.WriteLine(message);
                }
                catch (Exception e)
                {
                    Console.WriteLine(e.StackTrace);
                }
            }

            public void closeConnection()
            {
                try
                {
                    safeClose = true;
                    socket.Close();
                }
                catch (Exception) { }
            }
        }

        private void gameOutput()
        {
            Console.WriteLine("==============================");
            Console.WriteLine("Your ID: " + id);
            Console.WriteLine("Connected clients: ");
            foreach (int id in clientIds)
            {
                if (id == ball)
                {
                    Console.WriteLine("    Client " + id + " <-- Has the ball!");
                }
                else
                {
                    Console.WriteLine("    Client " + id);
                }
            }

            Console.WriteLine("==============================");
            if (ball == id)
            {
                Console.WriteLine("You have the ball!");
            }
            else
            {
                Console.WriteLine("Another player has the ball! \nWait for the ball to be passed to you.");
            }
        }

        static void Main(string[] args)
        {
            new Client();
        }
    }
}
