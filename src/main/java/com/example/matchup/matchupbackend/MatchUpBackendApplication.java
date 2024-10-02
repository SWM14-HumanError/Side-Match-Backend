package com.example.matchup.matchupbackend;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@RequiredArgsConstructor
@SpringBootApplication
public class MatchUpBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchUpBackendApplication.class, args);
    }

}
