package org.wanxp.spring.aop.service;

import org.springframework.cache.annotation.Cacheable;
import org.wanxp.spring.aop.entity.Book;

public class BookServiceImpl implements BookService{
    @Cacheable(value = "books", key = "#name", unless = "#a0=='Foundation'")
    @Override
    public Book getBookByName(String name) {
        return null;
    }
}
