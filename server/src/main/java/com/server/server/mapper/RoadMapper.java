package com.server.server.mapper;

import com.server.server.data.Road;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoadMapper {

    // 插入道路数据，包括起点名称、经纬度、距离、价格等信息
    @Insert("INSERT INTO roads (start_name, name, status, max_load, start_lat, start_long, end_lat, end_long, distance, price, next_roadid) " +
            "VALUES (#{startName}, #{name}, #{status}, #{maxLoad}, #{startLat}, #{startLong}, #{endLat}, #{endLong}, #{distance}, #{price}, #{nextRoadId})")
    void insertRoad(Road road);

    // 根据道路 ID 查询道路信息
    @Select("SELECT * FROM roads WHERE id = #{id}")
    Road getRoadById(int id);

    // 查询所有道路信息
    @Select("SELECT * FROM roads")
    List<Road> getAllRoads();

    // 根据道路 ID 查询道路的最大负载
    @Select("SELECT max_load FROM roads WHERE id = #{id}")
    int getMaxLoad(int id);

    // 更新道路状态
    @Update("UPDATE roads SET status = #{status} WHERE id = #{id}")
    void updateRoadStatus(int id, String status);

    // 更新道路的价格
    @Update("UPDATE roads SET price = #{price} WHERE id = #{id}")
    void updateRoadPrice(int id, double price);

    // 更新下一道路 ID
    @Update("UPDATE roads SET next_roadid = #{nextRoadId} WHERE id = #{id}")
    void updateNextRoadId(int id, String nextRoadId);
}
