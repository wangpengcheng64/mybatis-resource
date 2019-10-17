package com.wpc.mapper;

import com.wpc.bean.User;

import java.util.List;

public interface UserMapper {

    User query(Long id);

    List<User> list();

    int insert(User user);

    int update(User user);

    int delete(Long id);

}
