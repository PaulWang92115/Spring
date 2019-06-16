package com.paul.servlet;

import com.paul.annotation.RequestMapping;
import com.paul.annotation.RequestParam;
import org.springframework.ioc.annotation.Controller;
import org.springframework.ioc.bean.AnnotationApplicationContext;
import org.springframework.ioc.bean.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DispatcherServlet extends HttpServlet {

    // 完整路径和 方法的 mapping
    private Map<String,Object> handleMapping = new ConcurrentHashMap<>();

    // 类路径和controller 的 mapping
    private Map<String,Object> controllerMapping = new ConcurrentHashMap<>();

    private Map<String,Object> beanFactory = new ConcurrentHashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        //实例化 IOC 容器
        ApplicationContext ctx = new AnnotationApplicationContext("applicationContext.xml");
        this.beanFactory = ((AnnotationApplicationContext) ctx).beanFactory;
        //上一步已经完成了 com.paul.demo.Controller，com.paul.service，respostry，autowired 等注解的扫描和注入
        //遍历容器

        for(Map.Entry<String,Object> entry:beanFactory.entrySet()){
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if(clazz.isAnnotationPresent(Controller.class)){
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                String classPath = requestMapping.value();
                Method[] methods = clazz.getMethods();
                for(Method method:methods){
                    if(method.isAnnotationPresent(RequestMapping.class)){
                        RequestMapping requestMapping2 = method.getAnnotation(RequestMapping.class);
                        String methodPath = requestMapping2.value();
                        String requestPath = classPath + methodPath;
                        handleMapping.put(requestPath,method);
                        controllerMapping.put(requestPath,instance);
                    }else{
                        continue;
                    }

                }
            }else{
                continue;
            }
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();   //   /paul-mvc/com.paul.controller/method-com.paul.controller
        String context = req.getContextPath();  // /paul-vmc
        String path = uri.replace(context,"");  // /com.paul.controller/method-com.paul.controller
        Method m = (Method) handleMapping.get(path);

        //从容器里拿到controller 实例
        Object instance = controllerMapping.get(path);

        Object[] args =  handle(req,resp,m);
        for (Object a:args){
            System.out.println("Object:"+a);
        }

        try {
            m.invoke(instance,args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }


    private static Object[] handle(HttpServletRequest req, HttpServletResponse resp,Method method){
        //拿到当前执行的方法有哪些参数
        Class<?>[] paramClazzs = method.getParameterTypes();
        //根据参数的个数，new 一个参数的数据
        Object[] args = new Object[paramClazzs.length];

        int args_i = 0;
        int index = 0;
        for(Class<?> paramClazz:paramClazzs){
            if(ServletRequest.class.isAssignableFrom(paramClazz)){
                args[args_i++] = req;
            }
            if(ServletResponse.class.isAssignableFrom(paramClazz)){
                args[args_i++] = resp;
            }

            //判断requestParam  注解
            Annotation[] paramAns = method.getParameterAnnotations()[index];
            if(paramAns.length > 0){
                System.out.println("my");
                for(Annotation paramAn:paramAns){
                    if(RequestParam.class.isAssignableFrom(paramAn.getClass())){
                        System.out.println("13mj");
                        RequestParam rp = (RequestParam) paramAn;
                        args[args_i++] = req.getParameter(rp.value());
                    }
                }
            }
            index ++;
        }
        return  args;
    }

}
