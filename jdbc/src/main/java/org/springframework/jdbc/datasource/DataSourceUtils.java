package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 4단계 미션에서 사용할 것
public abstract class DataSourceUtils {

    private DataSourceUtils() {
    }

    public static Connection getConnection(DataSource dataSource) throws CannotGetJdbcConnectionException {
        Connection connection = TransactionSynchronizationManager.getResource(dataSource);
        if (connection != null) {
            return connection;
        }

        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
        }
    }

    public static void releaseConnection(Connection connection, DataSource dataSource) {
        if(isInTransaction(connection, dataSource)) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ex) {
            throw new CannotGetJdbcConnectionException("Failed to close JDBC Connection");
        }
    }

    private static boolean isInTransaction(final Connection connection, final DataSource dataSource) {
        return TransactionSynchronizationManager.getResource(dataSource) == connection;
    }
}
