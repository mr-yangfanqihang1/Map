package com.server.server.mapper;
import com.server.server.data.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserMapper {
    
    @Insert("INSERT INTO user (username, location, preferences) VALUES (#{username}, #{location}, #{preferences})")
    void insertUser(User user);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User getUserById(int id);

    @Select("SELECT * FROM user")
    List<User> getAllUsers();
}

