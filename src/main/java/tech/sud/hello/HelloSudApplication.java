package tech.sud.hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the demo and lets Spring wire controllers, services, and the SDK bean. */
@SpringBootApplication
public class HelloSudApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelloSudApplication.class, args);
    }
}
