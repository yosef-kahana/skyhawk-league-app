package com.skyhawk.league.repository;

import java.sql.SQLException;
import java.util.List;

public interface BaseRepository<T> {

    void createTable() throws SQLException;

    long saveIfNotExists(T entity) throws SQLException;

    List<T> getAll() throws SQLException;

    T getById(long id) throws SQLException;

    T getByName(String name) throws SQLException;
}
