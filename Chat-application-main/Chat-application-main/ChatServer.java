import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

public class ChatServer {

    private final JFrame frame = new JFrame("Chat Server");
    private final JTextArea logArea = new JTextArea(20, 50);
    private final JTextField inputField = new JTextField(40);
    private final JButton sendButton = new JButton("Send");

    private static final int PORT = 12345;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public ChatServer() {
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        sendButton.addActionListener(e -> sendServerMessage());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(this::startServer);
    }

    private void sendServerMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            String serverMessage = "[Server]: " + message;
            log(serverMessage);
            broadcast(serverMessage);
            inputField.setText("");
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    private void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    private void startServer() {
        log("Server started on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                handler.start();
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        }
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private String username;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message is the username
                this.username = in.readLine();
                log(username + " joined the chat.");
                broadcast("🔔 " + username + " joined the chat.");

                clients.add(this);

                String message;
                while ((message = in.readLine()) != null) {
                    String taggedMessage = "[" + username + "]: " + message;
                    log(taggedMessage);
                    broadcast(taggedMessage);
                }
            } catch (IOException e) {
                log("Connection with " + username + " lost.");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {}
                clients.remove(this);
                broadcast("❌ " + username + " left the chat.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatServer::new);
    }
}