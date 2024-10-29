package com.server.server.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.server.server.data.Road;
import com.server.server.data.Route;
import com.server.server.handler.JsonTypeHandler;

@Mapper
public interface RouteMapper {

    // 插入新路径
    @Insert("INSERT INTO routes (user_id, start_id, end_id, distance, duration, price, timestamp, priority, request_time, distance_weight, duration_weight, price_weight, route_data) " +
            "VALUES (#{userId}, #{startId}, #{endId}, #{distance}, #{duration}, #{price}, #{timestamp}, #{priority}, #{requestTime}, #{distanceWeight}, #{durationWeight}, #{priceWeight}, #{routeDataJson})")
    void insertRoute(Route route);

    @Select("SELECT * FROM routes WHERE id = #{id}")
    @Results({
        @Result(column = "route_data", property = "routeData", typeHandler = JsonTypeHandler.class)
        })
    Route getRouteById(int id);


    // 获取 Road 通过 ID
    @Select("SELECT * FROM roads WHERE id = #{id}")
    Road getRoadById(long id);

    @Select("SELECT user_id FROM routes WHERE JSON_CONTAINS(route_data, JSON_OBJECT('roadId', #{roadId}), '$')")
    List<Integer> getUsersByRoadId(@Param("roadId") long roadId);

    // 获取邻居 Road
    @Select("SELECT * FROM roads WHERE FIND_IN_SET(id, (SELECT next_roadid FROM roads WHERE id = #{roadId}))")
    List<Road> getNeighbors(long roadId);

    @Select("SELECT * FROM roads WHERE FIND_IN_SET(id, (SELECT next_roadid FROM roads WHERE id = #{roadId} AND status = 'green'))")
    List<Road> getGreenNeighbors(long roadId);

    // 根据起始和结束 ID 查找路径
    @Select("SELECT * FROM routes " +
    "WHERE start_id = #{startId} AND end_id = #{endId} " +
    "AND distance_weight = #{distanceWeight} " +
    "AND duration_weight = #{durationWeight} " +
    "AND price_weight = #{priceWeight} " +
    "LIMIT 1")
    Route findPathByStartAndEnd(
        @Param("startId") long startId,
        @Param("endId") long endId,
        @Param("distanceWeight") int distanceWeight,
        @Param("durationWeight") int durationWeight,
        @Param("priceWeight") int priceWeight
    );


    @Update("UPDATE routes SET user_id = #{userId}, start_id = #{startId}, end_id = #{endId}, " +
            "distance = #{distance}, duration = #{duration}, price = #{price}, timestamp = #{timestamp}, " +
            "priority = #{priority}, request_time = #{requestTime}, distance_weight = #{distanceWeight}, " +
            "duration_weight = #{durationWeight}, price_weight = #{priceWeight}, route_data = #{routeDataJson} " +
            "WHERE id = #{id}")
    void updateRoute(Route route);
}
