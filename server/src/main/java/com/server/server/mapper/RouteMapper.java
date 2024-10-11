package com.server.server.mapper;
import com.server.server.data.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RouteMapper {

    @Insert("INSERT INTO routes (user_id, start_point, end_point, distance, duration, path_data, timestamp) " +
            "VALUES (#{userId}, #{startPoint}, #{endPoint}, #{distance}, #{duration}, #{pathData, typeHandler=com.server.server.handler.JsonTypeHandler}, #{timestamp})")
    void insertRoute(Route route);

    @Select("SELECT * FROM routes WHERE id = #{id}")
    Route getRouteById(int id);
}

