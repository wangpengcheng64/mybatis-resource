package com.wpc.interceptor;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Sql操作拦截器
 */
@Intercepts({
        //@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SqlInterceptor implements Interceptor {

    /**
     * 数据库类型
     */
    private String databaseType;

    /**
     * 操作sql分页两种方式：
     * 1，拦截Executor的query方法，获取CacheKey及BoundSql，主动调用6个参数的query继续执行
     * 2，拦截StatementHandler的prepare，对StatementHandler里的BoundSql及RowBounds进行修改
     *
     * @param invocation invocation
     * @return 执行结果
     * @throws Throwable 异常
     */
    public Object intercept(Invocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        if ("query".equals(method.getName())) {
            Object parameterObject = invocation.getArgs()[1];
            List<Integer> paramsList = this.paramsAnalyze(parameterObject);
            if (null != paramsList) {
                Executor executor = (Executor) invocation.getTarget();
                MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
                RowBounds rowBounds = (RowBounds) invocation.getArgs()[2];
                ResultHandler resultHandler = (ResultHandler) invocation.getArgs()[3];
                BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
                this.updateBoundSqlAndRowBounds(boundSql, rowBounds, paramsList);
                CacheKey cacheKey = executor.createCacheKey(mappedStatement, parameterObject, rowBounds, boundSql);
                return executor.query(mappedStatement, parameterObject, rowBounds, resultHandler, cacheKey, boundSql);
            }
        } else {
            RoutingStatementHandler routingStatementHandler = (RoutingStatementHandler) invocation.getTarget();
            BoundSql boundSql = routingStatementHandler.getBoundSql();
            Object parameterObject = boundSql.getParameterObject();
            List<Integer> paramsList = this.paramsAnalyze(parameterObject);
            if (null != paramsList) {
                StatementHandler statementHandler = (StatementHandler) this.getField(routingStatementHandler, "delegate");
                RowBounds rowBounds = (RowBounds) this.getField(statementHandler, "rowBounds");
                this.updateBoundSqlAndRowBounds(boundSql, rowBounds, paramsList);
            }
        }
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    public void setProperties(Properties properties) {
        this.databaseType = properties.getProperty("databaseType");
    }

    /**
     * BoundSql RowBounds属性赋值
     *
     * @param boundSql   sql绑定对象
     * @param rowBounds  分页对象
     * @param paramsList 分页参数
     */
    private void updateBoundSqlAndRowBounds(BoundSql boundSql, RowBounds rowBounds, List<Integer> paramsList) {
        setFieldValue(rowBounds, "offset", paramsList.get(0));
        setFieldValue(rowBounds, "limit", paramsList.get(1));
        // 修改boundSql属性
        String sql = boundSql.getSql();
        if ("mysql".equals(databaseType)){
            sql = sql + " limit " + paramsList.get(0) + "," + paramsList.get(1);
        } else if ("oracle".equals(databaseType)){
            System.out.println("-----------");
        }
        setFieldValue(boundSql, "sql", sql);
    }

    /**
     * 利用反射给对象字段赋值
     *
     * @param object    对象
     * @param fieldName 字段名
     * @param value     字段值
     */
    private void setFieldValue(Object object, String fieldName, Object value) {
        Class<?> aClass = object.getClass();
        try {
            Field declaredField = aClass.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            declaredField.set(object, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取对象字段，当前类没有就向上获取
     *
     * @param object    对象
     * @param fieldName 字段名
     * @return 字段
     */
    private Object getField(Object object, String fieldName) {
        try {
            Class<?> aClass = object.getClass();
            while (Object.class != aClass) {
                Field[] declaredFields = aClass.getDeclaredFields();
                for (Field f : declaredFields) {
                    if (f.getName().equals(fieldName)) {
                        Field field = aClass.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        return field.get(object);
                    }
                }
                aClass = aClass.getSuperclass();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 分页参数解析
     *
     * @param parameterObject 参数
     * @return 分页参数
     */
    private List<Integer> paramsAnalyze(Object parameterObject) {
        if (parameterObject instanceof Map) {
            Map params = (Map) parameterObject;
            Object size = params.get("size");
            Object page = params.get("page");
            if (null != size && null != page) {
                int offset = ((int) page - 1) * (int) size;
                return Arrays.asList(offset, (int) size);
            }
        }
        return null;
    }
}
