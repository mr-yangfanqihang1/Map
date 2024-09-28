package com.server.server.mapper;
import com.server.server.data.DispatchLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DispatchLogMapper {

    @Insert("INSERT INTO dispatch_logs (timestamp, action) VALUES (#{timestamp}, #{action})")
    void insertDispatchLog(DispatchLog log);

    @Select("SELECT * FROM dispatch_logs")
    List<DispatchLog> getAllLogs();
}
