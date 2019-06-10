package com.spring.demo.Dao;

import org.springframework.ioc.annotation.Repository;

@Repository("bookDao")
public class BookDaoImpl implements BookDao {

	@Override
	public void read() {
		System.out.println("我在读书");
	}

}
