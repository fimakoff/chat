package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

class ClientHandler implements Runnable{

    private Socket socket;
    private Server server;
    private AuthService authService;
    private DataOutputStream out;
    private DataInputStream in;
    private String nick;
    private List<String> blackList;

    @Override
    public void run() {
        try {
            authorization();
            read();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    String getNick() {
        return nick;
    }

    ClientHandler(Server server, Socket socket) {
        try {
            this.blackList = new CopyOnWriteArrayList<>();
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.authService = new AuthServiceImpl();
            this.socket = socket;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read() {
        try {
            while (true) {
                String str = in.readUTF();
                if (str.startsWith("/")) {
                    if (str.equalsIgnoreCase("/end")) {
                        sendMsg("/serverclosed");
                        break;
                    }
                    // /w nick
                    if (str.startsWith("/w ")) {
                        String[] tokens = str.split(" ", 3);
                        authService.setBlackList(0, tokens[1]);
                        if (!checkBlackList(tokens[1])) {
                            server.sendPersonalMsg(this, tokens[1], tokens[2]);
                        }
                    }
                    // /blacklist nick
                    if (str.startsWith("/blacklist ")) {
                        String[] tokens = str.split(" ");
                        if(!checkBlackList(this.toString())) {
                            blackList.add(tokens[1]);
                            authService.setBlackList(1, tokens[1]);
                            System.out.println(Arrays.toString(blackList.toArray()));
//                        sendMsg("Вы добавили пользователя с ником " + tokens[1] +
//                                " в черный список!");
                            server.broadcast(this, nick + " добавил пользователя с ником "
                                    + tokens[1] + " в черный список!");
                        }
                    }
                    // /unblacklist nick
                    if (str.startsWith("/unblacklist ")) {
                        String[] tokens = str.split(" ",2);
                        if (!this.equals(tokens[1])) {                      // ПРАВИТЬ ЗДЕСЬ!!!!!
                            blackList.remove(tokens[1]);
                            authService.setBlackList(0, tokens[1]);
                            System.out.println(Arrays.toString(blackList.toArray()));
//                        sendMsg("Вы удалили пользователя с ником " + tokens[1] +
//                                " из черного списка!");
                            server.broadcast(this, nick + " удалил пользователя с ником " + tokens[1]
                                    + " из черного списка!");
                        }
                    }
                    // /updatenick login password newnick
                    if (str.startsWith("/updatenick ")) {
                        String[] tokens = str.split(" ",4);
                        authService.setNick(tokens[1], tokens[2], tokens[3]);
                        nick = authService.getNick(tokens[1], tokens[2]);
                        sendMsg("Вы изменили свой ник на " + nick);
                        server.unsubscribe(this);
                        server.subscribe(this);
                    }
                    // /help
                    if (str.startsWith("/help")){
                        String helpStr = "Доступные команды, реализованные для использования в данном чате:\n" +
                                "/w \'nick\' - отправить личное сообщение пользователю с ником \'nick\'\n" +
                                "/blacklist \'nick\' - добавить пользователя с ником \'nick\' в черный список\n" +
                                "/unblacklict \'nick\' - удалить пользователя с ником \'nick\' из черного списка\n" +
                                "/updatenick \'login\' \'password\' - поменять свой ник\n" +
                                "/end - закрыть соединение с сервером";
                        sendMsg(helpStr);
                    }
                } else {
                    server.broadcast(this, nick + " " + str);
                }
                System.out.println(nick + " " + str);
                Server.LOGGER.log(Level.INFO, nick + " " + str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void authorization() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/auth")) {
                String[] tokens = str.split(" ");
                String newNick = authService.getNick(tokens[1], tokens[2]);
                if (newNick != null) {
                    if (!server.isNickBusy(newNick)) {
                        sendMsg("/authOK");
                        nick = newNick;
                        server.subscribe(this);
                        break;
                    } else {
                        sendMsg("Учетная запись уже используется!");
                    }
                } else {
                    sendMsg("Неверный логин/пароль");
                }
            }
        }
    }

    private void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Server.LOGGER.log(Level.WARNING, "Попытка закрыть закрытое соединение");
        }
        server.unsubscribe(this);
    }

    void sendMsg(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
            Server.LOGGER.log(Level.WARNING, "Не удалось отправить сообщение!");
        }
    }

    boolean checkBlackList(String nick) {
//        return blackList.contains(nick);
        return authService.isBlackList(nick);
    }
}