package com.paul.demo.Controller;

import com.paul.demo.Service.UserService;
import com.paul.annotation.RequestMapping;
import com.paul.annotation.RequestParam;
import org.springframework.ioc.annotation.Autowired;
import org.springframework.ioc.annotation.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/query")
    public void get(HttpServletRequest request, HttpServletResponse response, @RequestParam("name") String name,@RequestParam("age") String age){
        try {
            PrintWriter pw = response.getWriter();
            String res = userService.query(name,age);
            pw.write(res);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
