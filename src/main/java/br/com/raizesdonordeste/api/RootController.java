package br.com.raizesdonordeste.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/api-docs").build();
    }
}
