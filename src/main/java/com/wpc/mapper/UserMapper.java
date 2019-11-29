package com.wpc.mapper;

import com.wpc.bean.User;

import java.util.List;
import java.util.Map;

public interface UserMapper {

    User query(Long id);

    List<User> list(Map<String, Object> params);

    int insert(User user);

    int update(User user);

    int delete(Long id);

}
