package com.fourverr.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FourverrApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FourverrApiApplication.class, args);
        System.out.println("¡FOURVERR API HA INICIADO CON ÉXITO!");
    }
}