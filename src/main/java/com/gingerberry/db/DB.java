package com.gingerberry.db;

import com.gingerberry.Config;

import java.lang.Class;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

public class DB {
    public static final String DB_NAME = "gingerberry";

    private static DB instance;

    private Connection conn;

    private DB() throws ClassNotFoundException, SQLException {
        String jdbcURL = "jdbc:mysql://" + Config.DB_HOST + "/" + Config.DB_NAME + "?user=" + Config.DB_USR
                + "&password=" + Config.DB_PWD
                + "&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

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