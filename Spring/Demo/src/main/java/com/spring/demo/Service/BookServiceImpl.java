package com.spring.demo.Service;

import org.springframework.ioc.annotation.Autowired;
import org.springframework.ioc.annotation.Service;

import com.spring.demo.Dao.BookDao;

@Service("bookService")
public class BookServiceImpl implements BookService {

	@Autowired
	private BookDao bookDao;
	
	@Override
	public void action() {
		bookDao.read();
	}

}
