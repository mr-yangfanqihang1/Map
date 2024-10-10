package com.server.server.mapper;
import com.server.server.data.*;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoadMapper {
    
    // @Insert("INSERT INTO roads (name, status, load) VALUES (#{name}, #{status}, #{load})")
    // void insertRoad(Road road);
    @Insert("INSERT INTO roads (start_point, end_point, name, status, max_load) " +
    "VALUES (#{startPoint}, #{endPoint}, #{name}, #{status}, #{maxLoad})")
    void insertRoad(Road road);
    @Select("SELECT * FROM roads WHERE id = #{id}")
    Road getRoadById(int id);

    @Select("SELECT * FROM roads")
    List<Road> getAllRoads();
    @Select("SELECT max_load FROM roads WHERE id = #{id}")
    int getMaxLoad(int id);
    @Update("Update roads SET status=#{status} WHERE id=#{id}")
    void updateRoadStatus(int id,String status);

}
