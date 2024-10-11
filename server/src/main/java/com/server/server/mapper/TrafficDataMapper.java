package com.server.server.mapper;
import com.server.server.data.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface TrafficDataMapper {

    @Insert("INSERT INTO traffic_data (road_id, user_id, speed, timestamp) VALUES (#{roadId}, #{userId}, #{speed}, #{timestamp})")
    void insertTrafficData(TrafficData trafficData);
    
    @Update("UPDATE traffic_data SET speed = #{speed}, timestamp = #{timestamp}, road_id = #{roadId} WHERE user_id = #{userId}")
    void updateTrafficData(TrafficData trafficData);

    @Select("SELECT * FROM traffic_data WHERE road_id = #{roadId}")
    List<TrafficData> getTrafficDataByRoadId(int roadId);

    @Select("SELECT COUNT(DISTINCT user_id) FROM traffic_data WHERE road_id = #{roadId}")
    int getUserCountByRoadId(int roadId);

    @Select("SELECT t.road_id AS roadId, COUNT(t.user_id) AS userCount, r.max_load AS maxLoad, r.status   FROM traffic_data t JOIN roads r ON t.road_id = r.id GROUP BY t.road_id;")
    List<RoadTrafficData> getUserCountAndMaxLoadForAllRoads();
    

    // 批量插入
    void batchInsertTrafficData(@Param("trafficDataList") List<TrafficData> trafficDataList);

    // 批量更新
    void batchUpdateTrafficData(@Param("trafficDataList") List<TrafficData> trafficDataList);
}


