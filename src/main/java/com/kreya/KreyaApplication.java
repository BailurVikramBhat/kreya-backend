package com.kreya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    excludeName = {
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    }
)
public class KreyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KreyaApplication.class, args);
    }
}
