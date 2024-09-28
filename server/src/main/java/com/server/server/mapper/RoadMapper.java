package com.server.server.mapper;
import com.server.server.data.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RoadMapper {
    
    @Insert("INSERT INTO roads (name, status, load) VALUES (#{name}, #{status}, #{load})")
    void insertRoad(Road road);

    @Select("SELECT * FROM roads WHERE id = #{id}")
    Road getRoadById(int id);

    @Select("SELECT * FROM roads")
    List<Road> getAllRoads();
}
