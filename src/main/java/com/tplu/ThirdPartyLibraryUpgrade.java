package com.tplu;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RestController
public class
ThirdPartyLibraryUpgrade {
    public static void main(String[] args) {
        SpringApplication.run(ThirdPartyLibraryUpgrade.class, args);
    }
}
