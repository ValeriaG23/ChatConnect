import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Map<String, ClientHandler> clientHandlers = new HashMap<>();

    public static void main(String[] args) {
        final int PORT = 8086;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastMessage(String message, ClientHandler excludeClient) {
        for (ClientHandler clientHandler : clientHandlers.values()) {
            if (clientHandler != excludeClient) {
                clientHandler.sendMessage(message);
            }
        }
    }

    public static void sendPrivateMessage(String receiverName, String message, ClientHandler sender) {
        ClientHandler receiver = clientHandlers.get(receiverName);
        if (receiver != null) {
            receiver.sendMessage("[Private from " + sender.getClientName() + "]: " + message);
            sender.sendMessage("[Private to " + receiverName + "]: " + message);
        } else {
            sender.sendMessage("User '" + receiverName + "' not found.");
        }
    }

    public static void addClient(String clientName, ClientHandler clientHandler) {
        clientHandlers.put(clientName, clientHandler);
    }

    public static void removeClient(String clientName) {
        clientHandlers.remove(clientName);
    }

    public static boolean clientExists(String clientName) {
        return clientHandlers.containsKey(clientName);
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            out.println("Enter your name: ");
            clientName = in.readLine();

            while (ChatServer.clientExists(clientName)) {
                out.println("Name already taken. Enter another name: ");
                clientName = in.readLine();
            }

            ChatServer.addClient(clientName, this);

            System.out.println(clientName + " has joined the chat.");
            ChatServer.broadcastMessage(clientName + " has joined the chat.", this);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/w ")) {
                    String[] splitMessage = message.split(" ", 3);
                    if (splitMessage.length == 3) {
                        String receiverName = splitMessage[1];
                        String privateMessage = splitMessage[2];
                        ChatServer.sendPrivateMessage(receiverName, privateMessage, this);
                    } else {
                        out.println("Invalid private message format. Use: /w [user] [message]");
                    }
                } else {
                    System.out.println(clientName + ": " + message);
                    ChatServer.broadcastMessage(clientName + ": " + message, this);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(clientName + " has left the chat.");
            ChatServer.broadcastMessage(clientName + " has left the chat.", this);
            ChatServer.removeClient(clientName);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getClientName() {
        return clientName;
    }
}
