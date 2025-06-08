package com.skyhawk.league.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnector {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnector.class);

    private final String url;
    private final String user;
    private final String password;

    public DatabaseConnector(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
    	log.info("Start getConnection");
        Connection connection = DriverManager.getConnection(url, user, password);
    	log.info("End getConnection: return={}", connection);
        return connection;
    }
}
