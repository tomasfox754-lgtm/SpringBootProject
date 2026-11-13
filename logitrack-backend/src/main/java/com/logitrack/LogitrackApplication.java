package com.logitrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LogitrackApplication {
    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Bogota"));
        SpringApplication.run(LogitrackApplication.class, args);
        System.out.println("hola");
    }
}
