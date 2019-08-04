package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

class StartServer {

    public static void main(String[] args) {
        new Server();
    }
}

class Server {

    private List<ClientHandler> peers;
    private int poolSize = Runtime.getRuntime().availableProcessors();
    static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    Server() {
        LOGGER.setLevel(Level.ALL);
        try {
            Handler h = new FileHandler("src\\Logging_Server.txt");
            h.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(h);
        } catch (IOException e) {
            e.printStackTrace();
        }
        AuthService authService = new AuthServiceImpl();
        peers = new CopyOnWriteArrayList<>();
        ServerSocket serverSocket = null;
        Socket socket = null;
        ExecutorService pool = null;
        try {
            authService.connect();
            serverSocket = new ServerSocket(8181);
            System.out.println("Сервер запущен!");
            LOGGER.log(Level.INFO, "Сервер запущен!!!");
            while (true) {
                pool = Executors.newFixedThreadPool(poolSize);
                socket = serverSocket.accept();
                pool.execute(new ClientHandler(this, socket));
                System.out.println("Клиент подключился!");
                LOGGER.log(Level.INFO, "Клиент подключился!");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Подключение не удалось\n" + e.getMessage());
            pool.shutdown();
        } finally {
            try {
                socket.close();
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Подключение не удалось. Соединение закрыто.\n" + e.getMessage());
            }
            authService.disconnect();
        }
    }

    void broadcast(ClientHandler from, String message) {
        for (ClientHandler clientHandler : peers) {
            if (!clientHandler.checkBlackList(from.getNick())) {
                clientHandler.sendMsg(message);
            }
        }
    }

    void subscribe(ClientHandler clientHandler) {
        peers.add(clientHandler);
        broadcastClientList();
    }

    void unsubscribe(ClientHandler clientHandler) {
        peers.remove(clientHandler);
        broadcastClientList();
    }

    void sendPersonalMsg(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler clientHandler : peers) {
            if (clientHandler.getNick().equalsIgnoreCase(nickTo)) {
                clientHandler.sendMsg("FROM: " + from.getNick() + " SEND: " + msg);
                from.sendMsg("TO: " + nickTo + " SEND: " + msg);
                return;
            }
        }
        from.sendMsg("Клиент с ником: " + nickTo + " не найден в чате");
    }

    boolean isNickBusy(String nick) {
        for (ClientHandler clientHandler : peers) {
            if (clientHandler.getNick().equalsIgnoreCase(nick)) {
                return true;
            }
        }
        return false;
    }

    private void broadcastClientList() {
        StringBuffer sb = new StringBuffer();
        sb.append("/clientlist ");
        for (ClientHandler clientHandler : peers) {
            sb.append(clientHandler.getNick() + " ");
        }
        String out = sb.toString();
        for (ClientHandler clientHandler : peers) {
            clientHandler.sendMsg(out);
        }
        LOGGER.log(Level.INFO, out);
    }
}