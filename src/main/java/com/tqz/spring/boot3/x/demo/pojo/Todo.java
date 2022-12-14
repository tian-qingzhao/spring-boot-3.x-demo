package com.tqz.spring.boot3.x.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>mock接口对应的实体类,<br>
 * 详见地址: <a href="https://jsonplaceholder.typicode.com"><i>https://jsonplaceholder.typicode.com</i></a>
 *
 * @author tianqingzhao
 * @since 2022/12/12 14:22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Todo {
    
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private Boolean completed;
}
