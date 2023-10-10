package org.springframework.transaction.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class TransactionManager {

    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void execute(Runnable runnable) {
        withTransaction(() -> {
            runnable.run();
            return null;
        });
    }

    private <T> T withTransaction(Supplier<T> supplier) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            conn.setAutoCommit(false);
            T result = supplier.get();
            conn.commit();
            return result;
        } catch (Exception e) {
            rollback(conn);
            throw new DataAccessException(e);
        } finally {
            close(conn);
        }
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private void close(Connection conn) {
        try {
            conn.setAutoCommit(true);
            DataSourceUtils.releaseConnection(conn, dataSource);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public <T> T executeWithValue(Supplier<T> supplier) {
        return withTransaction(supplier);
    }
}
