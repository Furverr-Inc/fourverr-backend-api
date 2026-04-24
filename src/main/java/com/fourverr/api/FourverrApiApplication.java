package com.fourverr.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FourverrApiApplication {

    private static final Logger log = LoggerFactory.getLogger(FourverrApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(FourverrApiApplication.class, args);
        log.info("¡FOURVERR API HA INICIADO CON ÉXITO!");
    }
}