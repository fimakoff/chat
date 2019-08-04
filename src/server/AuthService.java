package server;

import java.sql.*;

interface AuthService {

    String getNick(String login, String pass);

    void connect();

    void disconnect();

    void setNick(String login, String password, String newNick);

    void setBlackList(int sbl, String nick);

    boolean isBlackList(String nick);
}

class AuthServiceImpl implements AuthService {

    private static Connection connection;
    private static Statement statement;

    @Override
    public void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:db");
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setNick(String login, String password, String newNick) {
        String nick = getNick(login, password);
        String query = String.format("UPDATE users SET nick = '%s' WHERE nick = '%s'", newNick, nick);
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setBlackList(int sbl, String nick) {
        String query = String.format("UPDATE users SET isBlackList = '%d' WHERE nick = '%s'", sbl, nick);
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isBlackList(String nick) {
        String query = String.format("SELECT nick FROM users WHERE nick = '%s' AND isBlackList = '1'", nick);
        try {
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getNick(String login, String pass) {
        String query = String.format("select nick from users where login = '%s' and password = '%s'", login, pass);
        try {
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}