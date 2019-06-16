package com.paul.demo.Service;

import org.springframework.ioc.annotation.Service;

@Service("userService")
public class UserServiceImpl implements UserService {
    @Override
    public String query(String name, String age) {
        return "name="+name+"age="+age;
    }
}
