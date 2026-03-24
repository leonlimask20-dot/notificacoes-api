package com.leonlima.notificacoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NotificacoesApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificacoesApiApplication.class, args);
    }
}
