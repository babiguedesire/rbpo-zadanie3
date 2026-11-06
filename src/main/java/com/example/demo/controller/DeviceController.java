package com.example.demo.controller;

import com.example.demo.dto.DeviceRequest;
import com.example.demo.model.*;
import com.example.demo.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @GetMapping
    public List<Device> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Device getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Device create(@RequestBody DeviceRequest request) {
        return service.create(toEntity(request));
    }

    @PatchMapping("/{id}")
    public Device update(@PathVariable Long id, @RequestBody DeviceRequest request) {
        return service.update(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // Маппинг запроса в сущность (поля, которых нет в запросе, остаются null)
    private Device toEntity(DeviceRequest r) {
        Device e = new Device();
        e.setOwnerId(r.ownerId());
        e.setType(r.type());
        e.setModel(r.model());
        e.setSerialNumber(r.serialNumber());
        return e;
    }
}
