package com.server.server.handler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.data.RouteData;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.sql.*;

public class JsonTypeHandler extends BaseTypeHandler<List<RouteData>> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<RouteData> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, mapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("设置 JSON 参数出错", e);
        }
    }

    @Override
    public List<RouteData> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<RouteData> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<RouteData> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<RouteData> parseJson(String json) throws SQLException {
        try {
            return json == null ? null : mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, RouteData.class));
        } catch (JsonProcessingException e) {
            throw new SQLException("解析 JSON 出错", e);
        }
    }
}
