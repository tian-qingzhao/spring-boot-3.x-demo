package com.tqz.spring.boot3.x.demo.config;

import com.tqz.spring.boot3.x.demo.httpinterface.TodoClient;
import com.tqz.spring.boot3.x.demo.pojo.Todo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * <p>http interface 配置类。
 *
 * @author tianqingzhao
 * @since 2022/12/13 15:22
 */
@Configuration
@Slf4j
public class HttpInterfaceConfig {
    
    /**
     * 负载均衡通过服务名从注册中心获取地址。
     */
    //    @Autowired
    //    private ReactorLoadBalancerExchangeFilterFunction reactorLoadBalancerExchangeFilterFunction;
    
    @Bean
    TodoClient todoClient() {
        // 1.ip或域名直连
        WebClient client = WebClient.builder().baseUrl("https://jsonplaceholder.typicode.com/").build();
        
        // 2.使用注册中心加负载均衡
        //        WebClient client = WebClient.builder().filter(reactorLoadBalancerExchangeFilterFunction)
        //        .baseUrl("http://provider-service/").build();
        
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).build();
        return factory.createClient(TodoClient.class);
    }
    
    @Bean
    CommandLineRunner clr(TodoClient todoClient) {
        return args -> {
            try {
                log.info(todoClient.getTodo(1).toString());
                log.info(todoClient.save(new Todo(null, 1L, "看书", false)).toString());
                System.out.println("`HttpInterfaceConfig` 配置类 `clr` beanMethod 启动成功");
            } catch (Throwable e) {
                System.out.println("`HttpInterfaceConfig` 配置类 `clr` beanMethod 启动失败");
            } finally {
                log.info("project started.");
            }
        };
    }
}
