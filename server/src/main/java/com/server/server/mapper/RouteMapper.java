package com.server.server.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.server.server.data.Road;
import com.server.server.data.Route;

@Mapper
public interface RouteMapper {

    // 插入新路径
    @Insert("INSERT INTO routes (user_id, start_id, end_id, distance, duration, price, timestamp, priority, request_time, distance_weight, duration_weight, price_weight, route_data) " +
            "VALUES (#{userId}, #{startId}, #{endId}, #{distance}, #{duration}, #{price}, #{timestamp}, #{priority}, #{requestTime}, #{distanceWeight}, #{durationWeight}, #{priceWeight}, #{pathDataJson})")
    void insertRoute(Route route);

    @Select("SELECT * FROM routes WHERE id = #{id}")
    Route getRouteById(int id);

    // 获取 Road 通过 ID
    @Select("SELECT * FROM roads WHERE id = #{id}")
    Road getRoadById(long id);

    @Select("SELECT user_id FROM routes WHERE JSON_CONTAINS(route_data, JSON_OBJECT('roadId', #{roadId}), '$')")
    List<Integer> getUsersByRoadId(@Param("roadId") long roadId);

    // 获取邻居 Road
    @Select("SELECT * FROM roads WHERE FIND_IN_SET(id, (SELECT next_roadid FROM roads WHERE id = #{roadId}))")
    List<Road> getNeighbors(long roadId);

    // 根据起始和结束 ID 查找路径
    @Select("SELECT * FROM routes WHERE start_id = #{startId} AND end_id = #{endId}")
    Route findPathByStartAndEnd(@Param("startId") long startId, @Param("endId") long endId);

    @Update("UPDATE routes SET user_id = #{userId}, start_id = #{startId}, end_id = #{endId}, " +
            "distance = #{distance}, duration = #{duration}, price = #{price}, timestamp = #{timestamp}, " +
            "priority = #{priority}, request_time = #{requestTime}, distance_weight = #{distanceWeight}, " +
            "duration_weight = #{durationWeight}, price_weight = #{priceWeight}, route_data = #{routeDataJson} " +
            "WHERE id = #{id}")
    void updateRoute(Route route);
}
