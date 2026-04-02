package com.tfg.gestionentregables;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionEntregablesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionEntregablesApplication.class, args);
    }

}
