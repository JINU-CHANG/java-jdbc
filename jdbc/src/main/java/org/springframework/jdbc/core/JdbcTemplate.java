package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);
    private static final int SINGLE_SIZE = 1;

    private final DataSource dataSource;

    public JdbcTemplate(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int update(String sql, Object... args) {
        log.debug("query : {}", sql);
        return execute(new SetPreparedStatementMaker(sql, args), new PreparedStatementUpdateExecuter());
    }

    private <T> T execute(
            PreparedStatementMaker pstmtMaker,
            PreparedStatementExecuter<T> pstmtExecuter
    ) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (
                PreparedStatement pstmt = pstmtMaker.makePreparedStatement(conn)
        ) {
            return pstmtExecuter.execute(pstmt);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        log.debug("query : {}", sql);
        return execute(new SetPreparedStatementMaker(sql, args), new PreparedStatementQueryExecuter<>(rowMapper));
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        final List<T> objects = query(sql, rowMapper, args);
        return getSingleObject(objects);
    }

    private <T> T getSingleObject(List<T> objects) {
        validateEmpty(objects);
        validateSingleSize(objects);
        return objects.iterator().next();
    }

    private <T> void validateEmpty(List<T> objects) {
        if (objects.isEmpty()) {
            throw new DataAccessException("조회 데이터가 존재하지 않습니다.");
        }
    }

    private <T> void validateSingleSize(List<T> objects) {
        if (objects.size() > SINGLE_SIZE) {
            throw new DataAccessException("조회 데이터가 한 개 이상 존재합니다.");
        }
    }
}
