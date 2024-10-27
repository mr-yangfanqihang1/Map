package com.server.server.mapper;
import com.server.server.data.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface UserMapper {
    
    @Insert("INSERT INTO user (username, preferences) VALUES (#{username},  #{preferences})")
    void insertUser(User user);
    @Update("UPDATE user SET username=#{username}, preferences=#{preferences} where id=#{id}")
    void updateUser(User user);
    @Select("SELECT * FROM user WHERE id = #{id}")
    User getUserById(int id);
    @Delete("DELETE  FROM user WHERE id = #{id}")
    User deleteUserById(int id);
    @Select("SELECT * FROM user")
    List<User> getAllUsers();
    @Update("UPDATE user SET preferences=#{preferences} where id = #{id}")
    void updatePreferences(int id,String preferences);
    void insertUsers(List<User> users);
}

