package org.springframework.jdbc.core;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcTemplate {
    private final DataSource dataSource;

    public JdbcTemplate(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T queryForObject(final String sql, final RowMapper<T> rowMapper, final Object... args) {
        final List<T> result = query(sql, rowMapper, args);
        return getSingleResult(result);
    }

    public <T> List<T> query(final String sql, final RowMapper<T> rowMapper, final Object... args) throws DataAccessException {
        return execute(
                conn -> conn.prepareStatement(sql),
                pstmt -> {
                    setValues(pstmt, args);
                    return getResults(rowMapper, pstmt.executeQuery());
                }
        );
    }

    private void setValues(final PreparedStatement pstmt, final Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            pstmt.setObject(i + 1, args[i]);
        }
    }

    private <T> List<T> getResults(final RowMapper<T> rowMapper, final ResultSet rs) throws SQLException {
        final List<T> result = new ArrayList<>();
        if (rs.next()) {
            result.add(rowMapper.mapRow(rs));
        }
        return result;
    }

    private <T> T getSingleResult(final List<T> results) {
        if (results.isEmpty()) {
            throw new DataAccessException("데이터를 찾을 수 없습니다.");
        }
        if (results.size() > 1) {
            throw new DataAccessException("결과값이 두 개 이상입니다.");
        }
        return results.get(0);
    }

    public void update(final String sql, final Object... args) {
        execute(
                conn -> conn.prepareStatement(sql),
                pstmt -> {
                    setValues(pstmt, args);
                    return pstmt.executeUpdate();
                }
        );
    }

    private <T> T execute(final PreparedStatementCreator psc, final PreparedStatementCallBack<T> action) {
        final Connection connection = DataSourceUtils.getConnection(dataSource);
        try (final PreparedStatement pstmt = psc.createPreparedStatement(connection)) {
            return action.doInPreparedStatement(pstmt);
        } catch (final SQLException e) {
            throw new DataAccessException(e);
        }
    }
}
