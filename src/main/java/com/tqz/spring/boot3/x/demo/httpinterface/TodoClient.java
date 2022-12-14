package com.tqz.spring.boot3.x.demo.httpinterface;

import com.tqz.spring.boot3.x.demo.pojo.Todo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * <p>http interface 声明式调用
 *
 * @author tianqingzhao
 * @since 2022/12/12 12:54
 */
@HttpExchange("todos")
public interface TodoClient {
    
    @GetExchange("/{todoId}")
    Todo getTodo(@PathVariable Integer todoId);
    
    @PostExchange
    Todo save(@RequestBody Todo todo);
}
