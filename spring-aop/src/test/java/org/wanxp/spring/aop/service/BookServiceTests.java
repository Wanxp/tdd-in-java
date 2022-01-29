package org.wanxp.spring.aop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.wanxp.spring.aop.entity.Book;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 参考 https://www.baeldung.com/spring-data-testing-cacheable
 * <p>
 * 验证AOP拦截功能
 * 第一步 引入spring的注解
 * @ContextConfiguration
 * @ExtendWith(SpringExtension.class)
 * 第二步 构建Mock对象 mockBean
 *         @Bean
 *         public BookService bookServiceMock() {
 *             return mock(BookServiceImpl.class);
 *         }
 * 第三步 通过Spring注入对象 proxyBean
 *     @Autowired
 *     private BookService bookService;
 * 第四步 验证功能
 * 反复调用        assertEquals(DUNE, bookService.getBookByName("Dune"));
 * 验证调用一次 verify(mock).getBookByName("Dune");
 *  这里验证mock方法后续都没调用过
 *         verifyNoMoreInteractions(mock);
 *
 *
 *
 */
@ContextConfiguration
@ExtendWith(SpringExtension.class)
public class BookServiceTests {

    private static final Book DUNE = new Book(UUID.randomUUID(), "Dune");
    private static final Book FOUNDATION = new Book(UUID.randomUUID(), "Foundation");

    /**
     * mock是Mockit构建的对象
     */
    private BookService mock;
    /**
     * bookService是spring的代理对象,封装的其实是mock对象
     */
    @Autowired
    private BookService bookService;



    @BeforeEach
    void setUp (){
        //获取原始对象
        //现状的bookService其实是spring aop代理对象
        //通过 AopTestUtils.getTargetObject aop 代理前的对象
        mock = AopTestUtils.getTargetObject(bookService);
        reset(mock);
        when(mock.getBookByName(eq("Foundation"))).thenReturn(FOUNDATION);
        when(mock.getBookByName(eq("Dune"))).thenReturn(DUNE).thenThrow(new RuntimeException("Should be cache"));
    }

    /**
     * 测试 bookService.getBookByName真实方法只会被调用一次， 后面的多次调用都是只走AOP即Cacheable注解了
     * 以下方法验证AOP确实生效了
     * 第一次 在没有缓存的情况下 调用bookService.getBookByName("Dune") 会执行
     */
    @Test
    public void givenMethodThatShouldInvoke_whenCallIt_thenMethodShouldInvokeOnce() {
        //第一次调用真实方法
        assertEquals(DUNE, bookService.getBookByName("Dune"));
        //验证mock方法确实被调用了，注意mock对象与bookService关系，，
        //因为第一次没有缓存，所以第一次 bookService.getBookByName()执行的时候走mock.getBookByName(),执行了mock
        //这里验证mock方法调用了一次
        verify(mock).getBookByName("Dune");
        //反复调用 bookService.getBookByName()走缓存Cache获取内容，mock方法不再调用
        assertEquals(DUNE, bookService.getBookByName("Dune"));
        assertEquals(DUNE, bookService.getBookByName("Dune"));
        //这里验证mock方法后续都没调用过
        verifyNoMoreInteractions(mock);
    }

    /**
     * 测试 bookService.getBookByName 注解 Cacheable属性 unless = "#a0=='Foundation'导致不缓存
     * 所以真实方法会被调用多次
     */
    @Test
    public void givenMethodThatShouldInvoke_whenCallItTrice_thenMethodShouldInvokeTrice() {
        assertEquals(FOUNDATION, bookService.getBookByName("Foundation"));
        assertEquals(FOUNDATION, bookService.getBookByName("Foundation"));
        assertEquals(FOUNDATION, bookService.getBookByName("Foundation"));
        //这里验证mock方法调用过三次，
        //那是因为bookService.getBookByName实现方法里对Foundation不缓存的，因为使用了条件 unless = "#a0=='Foundation'"
        verify(mock, times(3)).getBookByName("Foundation");
    }

    @EnableCaching
    @Configuration
    public static class CachingTestConfig {

        @Bean
        public BookService bookServiceMock() {
            return mock(BookServiceImpl.class);
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("books");
        }
    }


}
