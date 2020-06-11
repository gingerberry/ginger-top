package com.gingerberry.db;

import java.lang.Class;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

public class DB {
    public static final String DB_NAME = "gingerberry";

    private static DB instance;

    private Connection conn;

    private DB() throws ClassNotFoundException, SQLException {
        String username = "admin";
        String password = "gingerberry";
        String dbName = "gingerberry";
        String dbEndpoint = "gingerberry.cwch0ro4xne5.us-east-1.rds.amazonaws.com";
        String jdbcURL = "jdbc:mysql://" + dbEndpoint + "/" + dbName + "?user=" + username + "&password="
                + password + "&characterEncoding=UTF-8";

        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection(jdbcURL);
    }

    public static DB getInstance() throws ClassNotFoundException, SQLException {
        if (instance == null) {
            instance = new DB();
        }

        return instance;
    }

    public Connection getConnection() {
        return conn;
    }
}