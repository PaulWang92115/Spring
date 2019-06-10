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
