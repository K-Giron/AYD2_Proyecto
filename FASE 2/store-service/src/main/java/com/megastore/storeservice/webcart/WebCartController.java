package com.megastore.storeservice.webcart;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store/web-cart")
public class WebCartController {

    private final WebCartService service;

    public WebCartController(WebCartService service) {
        this.service = service;
    }

    @GetMapping
    public WebCartService.WebCartResponse fetch(@RequestParam String username) {
        return service.fetch(username);
    }
}
