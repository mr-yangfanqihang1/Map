package com.server.server.mapper;

import com.server.server.data.Road;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoadMapper {

    // 插入道路数据，包括起点名称、经纬度、距离、价格等信息
    @Insert("INSERT INTO roads (id,start_name, name, status, max_load, start_lat, start_long, end_lat, end_long, distance, price, next_roadid) " +
            "VALUES (#{id}, #{startName}, #{name}, #{status}, #{maxLoad}, #{startLat}, #{startLong}, #{endLat}, #{endLong}, #{distance}, #{price}, #{nextRoadId})")
    void insertRoad(Road road);
    @Update("UPDATE roads SET start_name = #{startName}, name = #{name}, status = #{status}, max_load = #{maxLoad}, " +
    "start_lat = #{startLat}, start_long = #{startLong}, end_lat = #{endLat}, end_long = #{endLong}, " +
    "distance = #{distance}, price = #{price}, next_roadid = #{nextRoadId} WHERE id = #{id}")
    void updateRoad(Road road);

    // 根据道路 ID 查询道路信息
    @Select("SELECT * FROM roads WHERE id = #{id}")
    Road getRoadById(long id);

    // 根据道路名称查询道路信息
    @Select("SELECT * FROM roads WHERE name = #{name}")
    Road findByName(String name);


    // 查询所有道路信息
    @Select("SELECT * FROM roads")
    List<Road> getAllRoads();


    // 根据道路 ID 查询道路的最大负载
    @Select("SELECT max_load FROM roads WHERE id = #{id}")
    int getMaxLoad(long id);
    @Select("SELECT COUNT(*) FROM roads WHERE id = #{id}")
    int existsById(long id);
    // 更新道路状态
    @Update("UPDATE roads SET status = #{status} WHERE id = #{id}")
    void updateRoadStatus(long id, String status);
    @Update("UPDATE roads SET duration = #{duration} WHERE id = #{id}")
    void updateRoadDuration(long id, double duration);
    // 更新道路的价格
    @Update("UPDATE roads SET price = #{price} WHERE id = #{id}")
    void updateRoadPrice(long id, double price);

    // 更新下一道路 ID
    @Update("UPDATE roads SET next_roadid = #{nextRoadId} WHERE id = #{id}")
    void updateNextRoadId(long id, String nextRoadId);



}
