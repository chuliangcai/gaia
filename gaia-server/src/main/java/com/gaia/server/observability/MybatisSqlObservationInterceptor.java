package com.gaia.server.observability;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;

/**
 * MyBatis-Plus SQL Observation 拦截器：把每条 SQL 自动包装为 Micrometer Observation，
 * 由已存在的 {@code micrometer-tracing-bridge-brave} 桥接到 Brave span，
 * 在 Zipkin UI 中显示为 {@code mysql.<insert|update|select|delete>} 节点。
 *
 * <p>实现 MyBatis-Plus 的 {@link InnerInterceptor} 接口，
 * 通过 {@link com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor#addInnerInterceptor}
 * 注册，业务侧 Mapper 不需要任何改动。</p>
 *
 * <p>技术取舍：MyBatis-Plus 在执行 {@code beforeQuery}/{@code beforeUpdate} 钩子后
 * 立即同步执行真实 SQL，本拦截器无法精确观测真实 SQL 耗时。
 * 这里采用 {@code observation.observe(...)}：span 持续时间仅反映
 * 钩子自身开销（通常 1-3 ms），但足以让 Zipkin UI 看到 MySQL 调用节点，
 * 业务代码完全无侵入。</p>
 *
 * <p>如果需要精确的 SQL 执行耗时，请使用 OpenTelemetry Java Agent
 * （{@code -javaagent:opentelemetry-javaagent.jar}）。</p>
 */
@RequiredArgsConstructor
public class MybatisSqlObservationInterceptor implements InnerInterceptor {

    private static final int MAX_SQL_LENGTH = 256;

    private final ObservationRegistry observationRegistry;

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        recordObservation(ms, boundSql);
    }

    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameter);
        recordObservation(ms, boundSql);
    }

    /**
     * 同步开+关 Observation，使 span 出现在 Zipkin UI 中。
     */
    private void recordObservation(MappedStatement ms, BoundSql boundSql) {
        try {
            String rawSql = boundSql == null ? "" : boundSql.getSql();
            String sql = rawSql == null ? "" : rawSql.replaceAll("\\s+", " ").trim();
            if (sql.length() > MAX_SQL_LENGTH) {
                sql = sql.substring(0, MAX_SQL_LENGTH) + "...";
            }
            String command = parseSqlCommand(rawSql);
            String mapper = ms == null ? "unknown" : ms.getId();
            SqlCommandType type = ms == null ? SqlCommandType.UNKNOWN : ms.getSqlCommandType();

            Observation observation = Observation.createNotStarted(
                            "mysql." + (type == SqlCommandType.UNKNOWN ? "unknown" : type.name().toLowerCase()),
                            observationRegistry)
                    .lowCardinalityKeyValue("db.system", "mysql")
                    .lowCardinalityKeyValue("db.operation", command)
                    .lowCardinalityKeyValue("db.mapper", mapper)
                    .highCardinalityKeyValue("db.statement", sql);
            observation.observe(() -> null);
        } catch (Throwable t) {
            // 任何埋点异常都不应影响业务
        }
    }

    private static String parseSqlCommand(String sql) {
        if (sql == null) {
            return "UNKNOWN";
        }
        String trimmed = sql.trim();
        if (trimmed.isEmpty()) {
            return "UNKNOWN";
        }
        int idx = 0;
        while (idx < trimmed.length() && Character.isWhitespace(trimmed.charAt(idx))) {
            idx++;
        }
        int end = idx;
        while (end < trimmed.length() && !Character.isWhitespace(trimmed.charAt(end)) && trimmed.charAt(end) != '(') {
            end++;
        }
        if (end == idx) {
            return "UNKNOWN";
        }
        return trimmed.substring(idx, end).toUpperCase();
    }
}
