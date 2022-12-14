package com.tqz.spring.boot3.x.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>echo控制器
 *
 * @author tianqingzhao
 * @since 2022/12/12 20:31
 */
@RestController
public class EchoController {
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @RequestMapping("/")
    public String echo(@RequestParam(defaultValue = "hello-worle") String str) {
        String result = sdf.format(new Date()) + "  " + str;
        System.out.println(result);
        return result;
    }
}
