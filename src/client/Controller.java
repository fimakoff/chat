package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class Controller {

    @FXML
    TextField msgField;
    @FXML
    TextArea chatArea;
    @FXML
    HBox bottomPanel;
    @FXML
    HBox upperPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    ListView<String> clientList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String ADDRESS = "localhost";
    private final int PORT = 8181;

    private static boolean isAuthorized;
    private static String login;
    private static String password;

    private StringBuffer sb = new StringBuffer();
    private List<String> chatHistory = new ArrayList<>();
    private LinkedList<String> loadChatHistory = new LinkedList<>();
    private int numberOfLoadLines = 100;

    void timeOut(int time) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int i = time;

            @Override
            public void run() {
                System.out.println(Controller.isAuthorized + " " + i--);
                if (Controller.isAuthorized && i != 0) {
                    System.out.println("Client is already connected with login: \"" + login + "\" password: \"" + password + "\"");
                    timer.cancel();
                } else if (i == 0) {
                    System.exit(0);
                    System.out.println("Client is terminated");
                    close();
                }
            }
        }, 0, 1000);
    }

    private void setAuthorized(boolean isAuthorized) {
        Controller.isAuthorized = isAuthorized;
        if (!Controller.isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(true);
            clientList.setVisible(false);
            clientList.setManaged(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            clientList.setVisible(true);
            clientList.setManaged(true);
            Controller.isAuthorized = isAuthorized;
        }
    }

    private void connect() {
        try {
            socket = new Socket(ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    auth();
                    read();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read() throws IOException {
        readFromFile();
        while (true) {
            String str = in.readUTF();
            writeInFile(str);
            if (str.equalsIgnoreCase("/serverclosed")) {
                break;
            }
            if (str.startsWith("/clientlist") || str.startsWith("/updatenick")) {
                String[] tokens = str.split(" ");
                Platform.runLater(() -> {
                    clientList.getItems().clear();
                    for (int i = 1; i < tokens.length; i++) {
                        clientList.getItems().add(tokens[i]);
                    }
                });
            }
            chatArea.appendText(str + "\n");
        }
    }

    private void auth() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/authOK")) {
                setAuthorized(true);
                break;
            } else {
                chatArea.appendText(str + "\n");
            }
        }
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            login = loginField.getText().trim();
            password = passwordField.getText().trim();
            out.writeUTF("/auth " + login + " " + password);
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeInFile(String str) {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter("history_[" + login + "].txt", true));
            chatHistory.add(str + System.lineSeparator());
            writer.write(chatHistory.get(chatHistory.size() - 1));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFromFile() {
        try {
            File historyLogin = new File("history_[" + login + "].txt");
            BufferedReader reader = new BufferedReader(new FileReader(historyLogin));
            long startToRead = System.currentTimeMillis();
            String line = reader.readLine();
            while (line != null) {
                loadChatHistory.addFirst(line);
                line = reader.readLine();
            }
            int count = 0;
            if (numberOfLoadLines > loadChatHistory.size()) {
                for (int i = loadChatHistory.size() - 1; i >= 0; i--) {
                    if (loadChatHistory.get(i) != null) {
                        sb.append(loadChatHistory.get(i)).append(System.lineSeparator());
                        count++;
                    }
                }
            } else {
                for (int i = numberOfLoadLines - 1; i >= 0; i--) {
                    if (loadChatHistory.get(i) != null) {
                        sb.append(loadChatHistory.get(i)).append(System.lineSeparator());
                        count++;
                    }
                }
            }
            chatArea.appendText(sb + "\n");
            System.out.println("Time to load last " + count + " lines from " + loadChatHistory.size()
                    + " lines: " + (System.currentTimeMillis() - startToRead));
        } catch (FileNotFoundException e) {
            try {
                out.writeUTF("ББ приветствует тебя в чате! Логов не обнаружено.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.err.println("файла нет");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}