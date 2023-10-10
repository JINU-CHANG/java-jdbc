package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultSetException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidArgsException;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);
    private static final String QUERY_LOG = "query : {}";
    private static final int ONE_RESULT = 1;

    private final DataSource dataSource;

    public JdbcTemplate(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T queryForObject(String sql, Class<T> type, Object... args) {
        return execute(sql, psmt -> {
            ResultSet resultSet = executeWithArgs(psmt, sql, args);
            validOneResult(resultSet);
            return ObjectConverter.convertForObject(resultSet, type);
        });
    }

    private <T> T execute(String sql, PreparedStatementExecutor<T> preparedStatementExecutor) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement pstmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
        ) {
            log.debug(QUERY_LOG, sql);
            return preparedStatementExecutor.execute(pstmt);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    private ResultSet executeWithArgs(PreparedStatement pstmt, String sql, Object[] args) throws SQLException {
        setArgs(pstmt, sql, args);
        return pstmt.executeQuery();
    }

    private void setArgs(PreparedStatement pstmt, String sql, Object[] args) throws SQLException {
        long sqlArgsCount = countArgs(sql, '?');
        int providedArgsCount = args.length;
        validateArgs(sqlArgsCount, providedArgsCount);
        int argumentPoint = 1;
        for (Object arg : args) {
            pstmt.setObject(argumentPoint, arg);
            argumentPoint++;
        }
    }

    private long countArgs(String sql, char arg) {
        return sql.chars()
            .filter(c -> c == arg)
            .count();
    }

    private void validateArgs(long sqlArgsCount, int providedArgsCount) {
        if (sqlArgsCount != providedArgsCount) {
            throw new InvalidArgsException(String.format("Invalid Argument count required = %d but provided %d",
                sqlArgsCount,
                providedArgsCount));
        }
    }

    private void validOneResult(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            throw new IncorrectResultSizeDataAccessException("No rows selected");
        }
        resultSet.last();
        if (resultSet.getRow() != ONE_RESULT) {
            throw new IncorrectResultSizeDataAccessException("More than one row selected");
        }
        resultSet.beforeFirst();
    }

    public <T> List<T> queryForList(String sql, Class<T> type, Object... args) {
        return execute(sql, psmt -> {
            ResultSet resultSet = executeWithArgs(psmt, sql, args);
            return ObjectConverter.convertForList(resultSet, type);
        });
    }

    public int update(String sql, Object... args) {
        return execute(sql, psmt -> {
            setArgs(psmt, sql, args);
            return psmt.executeUpdate();
        });
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute(sql, psmt -> {
            ResultSet resultSet = executeWithArgs(psmt, sql, args);
            validOneResult(resultSet);
            if (resultSet.next()) {
                return rowMapper.mapRow(resultSet);
            }
            throw new EmptyResultSetException();
        });
    }

    public <T> List<T> queryForList(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute(sql, psmt -> {
            ResultSet resultSet = executeWithArgs(psmt, sql, args);
            List<T> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(rowMapper.mapRow(resultSet));
            }
            return result;
        });
    }
}
