package com.example.demo.controller;

import com.example.demo.dto.UserRequest;
import com.example.demo.model.*;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<User> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody UserRequest request) {
        return service.create(toEntity(request));
    }

    @PatchMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody UserRequest request) {
        return service.update(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // Маппинг запроса в сущность (поля, которых нет в запросе, остаются null)
    private User toEntity(UserRequest r) {
        User e = new User();
        e.setName(r.name());
        e.setEmail(r.email());
        return e;
    }
}
