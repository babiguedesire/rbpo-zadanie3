package com.example.demo.controller;

import com.example.demo.dto.SparePartRequest;
import com.example.demo.model.*;
import com.example.demo.service.SparePartService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spare-parts")
public class SparePartController {

    private final SparePartService service;

    public SparePartController(SparePartService service) {
        this.service = service;
    }

    @GetMapping
    public List<SparePart> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SparePart getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SparePart create(@RequestBody SparePartRequest request) {
        return service.create(toEntity(request));
    }

    @PatchMapping("/{id}")
    public SparePart update(@PathVariable Long id, @RequestBody SparePartRequest request) {
        return service.update(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // Маппинг запроса в сущность (поля, которых нет в запросе, остаются null)
    private SparePart toEntity(SparePartRequest r) {
        SparePart e = new SparePart();
        e.setName(r.name());
        e.setPrice(r.price());
        e.setStockQuantity(r.stockQuantity());
        e.setReservedQuantity(r.reservedQuantity());
        return e;
    }
}
