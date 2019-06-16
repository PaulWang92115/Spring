# 手动实现一个 IOC/DI 容器

* MySpring，自己实现的 IOC/DI 框架。
* PaulMVC，自己实现的 MVC 框架，依赖 MySpring。
* Demo，IOC/DI 框架的测试项目。
* WebDemo，MVC 框架和 IOC/DI 框架的 web 测试项目。

项目整体用 maven 构建，里面有两个模块，MySpring 为 IOC/DI 的核心，Demo 为测试项目。


1. 先来看看整体的项目结构，目前为第一个版本，好多需要完善的地方。最近好忙。
![](https://user-gold-cdn.xitu.io/2019/6/10/16b4191440468d07?w=736&h=1326&f=png&s=135736)
2. 首先我们把几个重要的注解定义出来。
   @Autowired，自动注入注解，用来实现 DI 功能。
   @Controller，控制层注解，暂时不实现 SpringMVC 相关的功能。后续分析完 SpringMVC 源码后会实现。
   @Service，服务层注解，与 Spring 中的 @Component 作用相同，就是将这个 Bean 交给 Spring 来管理。
   @Repository，持久层注解，将这个 Bean 交给 Spring 来管理。
   暂时先定义这几个注解。

   这几个注解的定义与代码都是一样的，就是将注解定义出来。
    
   ```java
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Service {
    
        String value() default "";
    }
   ```
3. 首先我们也定义一个 BeanFactory 工厂方法作为最上层的容器。里面主要有一个       getBean 方法用来从容器中获取 Bean，当然这里面已经包含了 Bean          
    的实例化过程。getBean 方法调用抽象的 doGetBean 方法，最后交给子类实现。

   ```java
    package org.springframework.ioc.factory;
    
    /**
     * 容器对象的工厂类，生产容器对象
     *
     */
    public abstract class BeanFactory {
    
        public Object getBean(String beanName){
            return doGetBean(beanName);
        }
    
        protected abstract Object doGetBean(String beanName);
    
    }    
   ```
4. 定义一个 ApplicationContext 继承 BeanFactory，在里面添加 xml      
   处理工具类和包的扫描路径。这个类也是一个抽象类。里面包含两个实例参数，配置文件路径和 xml 处理工具。
   ```java
    package org.paul.springframework.ioc.bean;
    
    import org.paul.springframework.ioc.factory.BeanFactory;
    import org.paul.springframework.ioc.xml.XmlUtil;
    
    public abstract class ApplicationContext extends BeanFactory {
    
        protected String configLocation;
        protected XmlUtil xmlUtil = null;
    
        public ApplicationContext(){
        }
    
        public ApplicationContext(String configLocations){
            this.configLocation = configLocations;
            xmlUtil = new XmlUtil();
        }
    
    }
   ```
5. 在看容器的最终实现类之前，我们先把 xmlUtil 和 配置文件的结构给大家看一下。
   xmlUtil 的作用就是解析配置文件获得类的所描路径。
   ```java
    package org.springframework.ioc.xml;
    
    import org.dom4j.Document;
    import org.dom4j.DocumentException;
    import org.dom4j.Element;
    import org.dom4j.io.SAXReader;
    import java.io.InputStream;
    
    /**
     * 解析容器的配置文件中扫描包的路径
     *
     */
    public class XmlUtil {
        public String handlerXMLForScanPackage(String configuration){
            InputStream ins = this.getClass().getClassLoader().getResourceAsStream(configuration);
            SAXReader reader = new SAXReader();
            try{
                Document document = reader.read(ins);
                Element root = document.getRootElement();
                Element element = root.element("package-scan");
                String res = element.attributeValue("component-scan");
                return res;
            }catch (DocumentException e){
                e.printStackTrace();
            }
            return null;
        }
    }

   ```
   
   ```xml
    <?xml version="1.0" encoding="UTF-8" ?>
    <beans>
        <package-scan component-scan="com.spring.demo" />
    </beans>
   ```
6. 容器的最终实现类，基于注解的 xml 扫描容器配置类。
   * 首先定义两个线程安全的 List 和 一个 ConcurrentHashMap，分别用来保存扫描   类的路径，类和实例对象。      
   * 在 AnnotationApplicationContext 的构造函数里，分别实现了以下的功能。
     调用父类的初始化方法，将 xml 工具实例化。
     使用 xmlUtil 和 配置文件路径获取到扫描的包路径。
     获取到包路径后，执行包的扫描操作。
     将里面有对应注解的 Bean 注入到容器中。
     将对象创建出来，先忽略依赖关系。
     执行容器实例管理和对象运行期间的依赖装配。
 
   ```java
    package org.springframework.ioc.bean;
    
    import java.io.File;
    import java.io.FileFilter;
    import java.lang.reflect.Field;
    import java.net.URISyntaxException;
    import java.net.URL;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;
    import java.util.Map;
    import java.util.Map.Entry;
    import java.util.concurrent.ConcurrentHashMap;
    
    import org.springframework.ioc.annotation.Autowired;
    import org.springframework.ioc.annotation.Component;
    import org.springframework.ioc.annotation.Controller;
    import org.springframework.ioc.annotation.Repository;
    import org.springframework.ioc.annotation.Service;
    
    public class AnnotationApplicationContext extends ApplicationContext {
    	
    	//保存类路径的缓存
    	private static List<String> classCache = Collections.synchronizedList(new ArrayList<String>());
    	
    	//保存需要注入的类的缓存
    	private static List<Class<?>> beanDefinition = Collections.synchronizedList(new ArrayList<Class<?>>());
    
    	//保存类实例的容器
    	private static Map<String,Object> beanFactory = new ConcurrentHashMap<>();
    	
    	public AnnotationApplicationContext(String configuration) {
    		super(configuration);
    		String path  = xmlUtil.handlerXMLForScanPackage(configuration);
            System.out.println(path);
            
            //执行包的扫描操作
            scanPackage(path);
            //注册bean
            registerBean();
            //把对象创建出来，忽略依赖关系
            doCreateBean();
            //执行容器管理实例对象运行期间的依赖装配
            diBean();
    	}
    
    
    
    	@Override
    	protected Object doGetBean(String beanName) {
    		return beanFactory.get(beanName);
    	}
    	
    	/**
    	 * 扫描包下面所有的 .class 文件的类路径到上面的List中
    	 * 
    	 */
    	private void scanPackage(final String path) {
    		URL url = this.getClass().getClassLoader().getResource(path.replaceAll("\\.", "/"));
    		try {
    			File file = new File(url.toURI());
    			
    			file.listFiles(new FileFilter(){
    				//pathname 表示当前目录下的所有文件
    				@Override
    				public boolean accept(File pathname) {
    					//递归查找文件
    					if(pathname.isDirectory()){
    						scanPackage(path+"."+pathname.getName());
    					}else{
    						if(pathname.getName().endsWith(".class")){
                                String classPath = path + "." + pathname.getName().replace(".class","");
                                classCache.add(classPath);
    						}
    					}
    					return true;
    				}
    				
    			});
    		} catch (URISyntaxException e) {
    			e.printStackTrace();
    		}
    		
    		
    	}
    	
    
    	/**
    	 * 根据类路径获得 class 对象
    	 */
    	private void registerBean() {
    		if(classCache.isEmpty()){
    			return;
    		}
    		
    		
    		for(String path:classCache){
    			try {
    				//使用反射，通过类路径获取class 对象
    				Class<?> clazz = Class.forName(path);
    				//找出需要被容器管理的类，比如，@Component，@Controller，@Service，@Repository
    				if(clazz.isAnnotationPresent(Repository.class)||clazz.isAnnotationPresent(Service.class)
    						||clazz.isAnnotationPresent(Controller.class)|| clazz.isAnnotationPresent(Component.class)){
    					beanDefinition.add(clazz);
    				}
    			} catch (ClassNotFoundException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		}
    		
    	}
    
    	
    	/**
    	 * 
    	 * 根据类对象，创建实例
    	 */
    	private void doCreateBean() {
    		if(beanDefinition.isEmpty()){
    			return;
    		}
    		
    
    		for(Class clazz:beanDefinition){
    			try {
    				Object instance = clazz.newInstance();
    				//将首字母小写的类名作为默认的 bean 的名字
    				String aliasName = lowerClass(clazz.getSimpleName());
    				//先判断@ 注解里面是否给了 Bean 名字，有的话，这个就作为 Bean 的名字
    				if(clazz.isAnnotationPresent(Repository.class)){
    					Repository repository = (org.springframework.ioc.annotation.Repository) clazz.getAnnotation(Repository.class);
    					if(!"".equals(repository.value())){
    						aliasName = repository.value();
    					}
    				}	
    				if(clazz.isAnnotationPresent(Service.class)){
    					Service service = (org.springframework.ioc.annotation.Service) clazz.getAnnotation(Service.class);
    					if(!"".equals(service.value())){
    						aliasName = service.value();
    					}
    				}
    				if(clazz.isAnnotationPresent(Controller.class)){
    					Controller controller = (org.springframework.ioc.annotation.Controller) clazz.getAnnotation(Controller.class);
    					if(!"".equals(controller.value())){
    						aliasName = controller.value();
    					}
    				}
    				if(clazz.isAnnotationPresent(Component.class)){
    					Component component = (org.springframework.ioc.annotation.Component) clazz.getAnnotation(Component.class);
    					if(!"".equals(component.value())){
    						aliasName = component.value();
    					}
    				}
    				if(beanFactory.get(aliasName)== null){
    					beanFactory.put(aliasName, instance);
    				}
    				
    				//判断当前类是否实现了接口
    				Class<?>[] interfaces = clazz.getInterfaces();
    				if(interfaces == null){
    					continue;
    				}
    				//把当前接口的路径作为key存储到容器中
    				for(Class<?> interf:interfaces){
    					beanFactory.put(interf.getName(), instance);
    				}
    				
    
    				
    			} catch (InstantiationException e) {
    				e.printStackTrace();
    			} catch (IllegalAccessException e) {
    				e.printStackTrace();
    			}
    		}
    		
    		
    		for (Entry<String, Object> entry : beanFactory.entrySet()) { 
    			  System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
    			}
    		
    		
    	}
    	
    	/**
    	 * 对创建好的对象进行依赖注入
    	 */
    	private void diBean() {
    		if(beanFactory.isEmpty()){
    			return;
    		}
    		
    		for(Class<?> clazz:beanDefinition){
    			String aliasName = lowerClass(clazz.getSimpleName());
    			//先判断@ 注解里面是否给了 Bean 名字，有的话，这个就作为 Bean 的名字
    			if(clazz.isAnnotationPresent(Repository.class)){
    				Repository repository = (org.springframework.ioc.annotation.Repository) clazz.getAnnotation(Repository.class);
    				if(!"".equals(repository.value())){
    					aliasName = repository.value();
    				}
    			}	
    			if(clazz.isAnnotationPresent(Service.class)){
    				Service service = (org.springframework.ioc.annotation.Service) clazz.getAnnotation(Service.class);
    				if(!"".equals(service.value())){
    					aliasName = service.value();
    				}
    			}
    			if(clazz.isAnnotationPresent(Controller.class)){
    				Controller controller = (org.springframework.ioc.annotation.Controller) clazz.getAnnotation(Controller.class);
    				if(!"".equals(controller.value())){
    					aliasName = controller.value();
    				}
    			}
    			if(clazz.isAnnotationPresent(Component.class)){
    				Component component = (org.springframework.ioc.annotation.Component) clazz.getAnnotation(Component.class);
    				if(!"".equals(component.value())){
    					aliasName = component.value();
    				}
    			}
    			
    			//根据别名获取到被装配的 bean 的实例
    			Object instance = beanFactory.get(aliasName);
    			try{
    				//从类中获取参数，判断是否有 @Autowired 注解
    				Field[] fields = clazz.getDeclaredFields();
    				for(Field f:fields){
    					if(f.isAnnotationPresent(Autowired.class)){
    						System.out.println("12312312312123");
    						//开启字段的访问权限
    						f.setAccessible(true);
    						Autowired autoWired = f.getAnnotation(Autowired.class);
    						if(!"".equals(autoWired.value())){
    							System.out.println("111111111111111111111");
    							//注解里写了别名
    							f.set(instance, beanFactory.get(autoWired.value()));
    							
    						}else{
    							//按类型名称
    							String fieldName = f.getType().getName();
    							f.set(instance, beanFactory.get(fieldName));
    						}
    					}
    				}
    			}catch(Exception e){
    				e.printStackTrace();
    			}
    			
    		}
    		
    	}
    
    
    	private String lowerClass(String simpleName) {
            char[] chars = simpleName.toCharArray();
            chars[0] += 32;
            return chars.toString();
    	}
    
    }



   ```
7. 在 Demo 模块中定义类似与 SpringMVC 的三层架构，并在 Service 层注入 Dao，在    Dao 层我们只打印了一句话，为了验证 DI 成功。
   ```java
    package org.Demo;

    import static org.junit.Assert.assertTrue;
    
    import org.junit.Test;
    import org.springframework.ioc.bean.AnnotationApplicationContext;
    import org.springframework.ioc.bean.ApplicationContext;
    
    import com.spring.demo.Service.BookService;
    
    /**
     * Unit test for simple App.
     */
    public class AppTest 
    {
    	public static void main(String[] args){
    
    		ApplicationContext ctx = new AnnotationApplicationContext("applicationContext.xml");
    		BookService bookService = (BookService) ctx.getBean("bookService");
    		bookService.action();
    	}
    }

   ```
8. 测试结果：
   ```xml
    // 容器中的 Bean
    Key = bookDao, Value = com.spring.demo.Dao.BookDaoImpl@3d494fbf
    Key = [C@4fca772d, Value = com.spring.demo.controller.BookController@1ddc4ec2
    Key = bookService, Value = com.spring.demo.Service.BookServiceImpl@133314b
    Key = com.spring.demo.Dao.BookDao, Value = com.spring.demo.Dao.BookDaoImpl@3d494fbf
    Key = com.spring.demo.Service.BookService, Value = com.spring.demo.Service.BookServiceImpl@133314b
    
    // service 调用 dao 层的方法，成功打印。
    我在读书
   ```
   
   这样一个  IOC/DI 容器就构建成功了，希望大家 mark 一下，一起改进。
   
   下面我们在这个基础上实现一个 MVC 框架。首先 @Controller，@Service，@Repository 注解已经在我们自己的 Spring 框架里面定义了，可以实现将被这些注解标记的类交给 Spring 管理，并且实现了依赖注入。

### 先看整体架构

![](https://user-gold-cdn.xitu.io/2019/6/16/16b5f96f6b0ee5df?w=894&h=854&f=png&s=94379)
 因为我们这个 MVC 框架要依赖 IOC/DI 容器，所以我们在 pom 文件里要将自己的 Spring 框架引入进来。
 
![](https://user-gold-cdn.xitu.io/2019/6/16/16b5f982d83c6f93?w=1774&h=494&f=png&s=114809)

### 实现 MVC 的整体功能
首先我们定义两个 MVC 专用的注解，RequestMapping 用来做 url 匹配，RequestParam 做参数转换：
```java
package com.paul.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}

```
```java
package com.paul.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    String value() default "";
}

```

我们知道 SpringMVC 的核心是 DispatcherServlet，用来做核心路由控制，我们也定义这样一个类，并且在初始化方法里初始化一个 IOC/DI 容器，看过前面文章的同学应该知道，初始化容器后我们已经将 Bean 放到容器中而且完成了依赖注入。

```java
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

//实现 servlet，和我们以前使用 servlet 一样。
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
        //上一步已经完成了 Controller，service，respostry，autowired 等注解的扫描和注入
        //遍历容器，将 requestmapping 注解的路径和对应的方法以及 contoller 实例对应起来

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

    //根据上一步获取到的 mapping，根据 url 找到对应的 controller 和方法去执行。
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

```

### 测试代码
新建一个 WebDemo，Maven web 项目。

先来看我们需要测试的 Controller 和 Service。
```java
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
```

```java
import org.springframework.ioc.annotation.Service;

@Service("userService")
public class UserServiceImpl implements UserService {
    @Override
    public String query(String name, String age) {
        return "name="+name+"age="+age;
    }
}
```

在 resources 目录我们需要写一个名字为 applicationContext 的配置文件来指明扫描包路径。
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <package-scan component-scan="com.paul.demo" />
</beans>
```
在 web.xml 中配置我们自己的 DispatcherServlet。
```java
<!DOCTYPE web-app PUBLIC
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
        "http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app>
  <display-name>Archetype Created Web Application</display-name>
  <servlet>
    <servlet-name>DispatcherServlet</servlet-name>
    <servlet-class>com.paul.servlet.DispatcherServlet</servlet-class>
    <load-on-startup>0</load-on-startup>
  </servlet>

  <servlet-mapping>
    <servlet-name>DispatcherServlet</servlet-name>
    <url-pattern>/</url-pattern>
  </servlet-mapping>
</web-app>

```

在浏览器中测试结果：

![](https://user-gold-cdn.xitu.io/2019/6/16/16b5fa4cfacd8bf8?w=1334&h=212&f=png&s=32254)

结果和我们想的一样。
