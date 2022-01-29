package org.wanxp.spring.aop.service;

import org.wanxp.spring.aop.entity.Book;

public interface BookService {
    Book getBookByName(String name);
}
