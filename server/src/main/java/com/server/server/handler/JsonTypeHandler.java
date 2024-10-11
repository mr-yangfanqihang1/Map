package com.server.server.handler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import com.server.server.data.*;
import java.sql.*;
import java.util.List;

public class JsonTypeHandler extends BaseTypeHandler<List<PathData>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<PathData> parameter, JdbcType jdbcType) throws SQLException {
        try {
            // 将 List<PathData> 转换为 JSON 字符串
            String json = objectMapper.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting List<PathData> to JSON string.", e);
        }
    }

    @Override
    public List<PathData> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toPathDataList(rs.getString(columnName));
    }

    @Override
    public List<PathData> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toPathDataList(rs.getString(columnIndex));
    }

    @Override
    public List<PathData> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toPathDataList(cs.getString(columnIndex));
    }

    private List<PathData> toPathDataList(String json) throws SQLException {
        try {
            if (json != null) {
                // 将 JSON 字符串转换为 List<PathData>
                return objectMapper.readValue(json, new TypeReference<List<PathData>>() {});
            }
        } catch (Exception e) {
            throw new SQLException("Error converting JSON string to List<PathData>.", e);
        }
        return null;
    }
}
