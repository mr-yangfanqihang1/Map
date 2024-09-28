package com.server.server.mapper;
import com.server.server.data.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RouteMapper {
    
    @Insert("INSERT INTO routes (start_point, end_point, path_data, timestamp) VALUES (#{startPoint}, #{endPoint}, #{pathData}, #{timestamp})")
    void insertRoute(Route route);

    @Select("SELECT * FROM routes WHERE id = #{id}")
    Route getRouteById(int id);
}
