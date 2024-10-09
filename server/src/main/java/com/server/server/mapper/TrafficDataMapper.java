package com.server.server.mapper;
import com.server.server.data.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TrafficDataMapper {
    
    @Insert("INSERT INTO traffic_data (road_id, user_id, speed, timestamp) VALUES (#{roadId}, #{userId}, #{speed}, #{timestamp})")
    void insertTrafficData(TrafficData trafficData);
    @Update("Update traffic_data (road_id, user_id, speed, timestamp) VALUES (#{roadId}, #{userId}, #{speed}, #{timestamp})")
    void updateTrafficData(TrafficData trafficData);
    @Select("SELECT * FROM traffic_data WHERE road_id = #{roadId}")
    List<TrafficData> getTrafficDataByRoadId(int roadId);
}

