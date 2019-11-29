package com.wpc.test;

import com.wpc.bean.User;
import com.wpc.mapper.UserMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserTest {

    public static void main(String[] args) {
        // 加载mybatis配置文件，输入流
        InputStream inputStream = UserTest.class.getClassLoader().getResourceAsStream("mybatis-config.xml");
        // 传入输入流，获取sqlSessionFactory对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
//        query(mapper);
        list(mapper);
//        update(mapper);
//        query(mapper);
//        insert(mapper);
//        delete(mapper);
    }

    private static void query(UserMapper mapper) {
        User user = mapper.query(1L);
        System.out.println(user);
    }

    private static void list(UserMapper mapper) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 2);
        List<User> list = mapper.list(params);
        for (User user : list) {
            System.out.println(user);
        }
    }

    private static void update(UserMapper mapper) {
        User user = new User();
        user.setId(3L);
        user.setName("zl");
        user.setAge(21);
        mapper.update(user);
    }

    private static void insert(UserMapper mapper) {
        User user = new User();
        user.setName("zl");
        user.setAge(21);
        mapper.insert(user);
    }

    private static void delete(UserMapper mapper) {
        mapper.delete(2L);
    }


}
