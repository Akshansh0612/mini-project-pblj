import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class ChatClient {

    private final JFrame frame = new JFrame("Java Chat Client");
    private final JTextArea messageArea = new JTextArea(20, 50);
    private final JTextField inputField = new JTextField(40);
    private final JButton sendButton = new JButton("Send");

    private final String serverAddress = "localhost";
    private final int port = 12345;
    private final String username;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ChatClient() {
        username = JOptionPane.showInputDialog(frame, "Enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);

        messageArea.setEditable(false);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        startClientThread();
    }

    private void startClientThread() {
        new Thread(this::startClient).start();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            out.println(text);
            inputField.setText("");
        }
    }

    private void startClient() {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send username first
            out.println(username);

            String msg;
            while ((msg = in.readLine()) != null) {
                messageArea.append(msg + "\n");
            }
        } catch (IOException e) {
            messageArea.append("Connection failed: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}
