package com.example.demo.controller;

import com.example.demo.dto.RepairItemRequest;
import com.example.demo.model.*;
import com.example.demo.service.RepairItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repair-items")
public class RepairItemController {

    private final RepairItemService service;

    public RepairItemController(RepairItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<RepairItem> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public RepairItem getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepairItem create(@RequestBody RepairItemRequest request) {
        return service.create(toEntity(request));
    }

    @PatchMapping("/{id}")
    public RepairItem update(@PathVariable Long id, @RequestBody RepairItemRequest request) {
        return service.update(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ==================== Бизнес-операции ====================

    /** Списание зарезервированной запчасти: RESERVED -> USED (склад и резерв уменьшаются). */
    @PostMapping("/{id}/use")
    public RepairItem use(@PathVariable Long id) {
        return service.use(id);
    }

    /** Отмена позиции: RESERVED -> CANCELLED (резерв возвращается на склад). */
    @PostMapping("/{id}/cancel")
    public RepairItem cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    // Маппинг запроса в сущность (поля, которых нет в запросе, остаются null)
    private RepairItem toEntity(RepairItemRequest r) {
        RepairItem e = new RepairItem();
        e.setOrderId(r.orderId());
        e.setSparePartId(r.sparePartId());
        e.setQuantity(r.quantity());
        e.setFixedPrice(r.fixedPrice());
        e.setUsageStatus(r.usageStatus());
        return e;
    }
}
