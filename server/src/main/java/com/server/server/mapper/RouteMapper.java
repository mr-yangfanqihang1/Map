package com.server.server.mapper;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.server.server.data.Road;
import com.server.server.data.Route;

@Mapper
public interface RouteMapper {

    @Insert("INSERT INTO routes (user_id, start_pointname, end_pointname, distance, duration, path_data, timestamp) " +
            "VALUES (#{userId}, #{startPoint}, #{endPoint}, #{distance}, #{duration}, #{pathData, typeHandler=com.server.server.handler.JsonTypeHandler}, #{timestamp})")
    void insertRoute(Route route);

    @Select("SELECT * FROM routes WHERE id = #{id}")
    Route getRouteById(int id);
        // 获取 Road 通过 ID
    @Select("SELECT * FROM roads WHERE id = #{id}")
    Road getRoadById(long id);

    // 获取邻居 Road
    // 获取邻居 Road
    @Select("SELECT * FROM roads WHERE FIND_IN_SET(id, (SELECT next_roadid FROM roads WHERE id = #{roadId}))")
    List<Road> getNeighbors(long roadId);
}

