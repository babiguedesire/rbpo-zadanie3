package com.example.demo.controller;

import com.example.demo.dto.RepairOrderRequest;
import com.example.demo.model.*;
import com.example.demo.service.RepairOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repair-orders")
public class RepairOrderController {

    private final RepairOrderService service;

    public RepairOrderController(RepairOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<RepairOrder> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public RepairOrder getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepairOrder create(@RequestBody RepairOrderRequest request) {
        return service.create(toEntity(request));
    }

    @PatchMapping("/{id}")
    public RepairOrder update(@PathVariable Long id, @RequestBody RepairOrderRequest request) {
        return service.update(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ==================== Бизнес-операции ====================

    /** Приёмка наряда в работу: NEW -> IN_PROGRESS. */
    @PostMapping("/{id}/start")
    public RepairOrder start(@PathVariable Long id) {
        return service.start(id);
    }

    /** Заказ ждёт поступления запчастей: IN_PROGRESS -> WAITING_FOR_PARTS. */
    @PostMapping("/{id}/wait-for-parts")
    public RepairOrder waitForParts(@PathVariable Long id) {
        return service.waitForParts(id);
    }

    /** Продолжение работ: WAITING_FOR_PARTS -> IN_PROGRESS. */
    @PostMapping("/{id}/resume")
    public RepairOrder resume(@PathVariable Long id) {
        return service.resume(id);
    }

    /** Завершение ремонта: стоимость считается по списанным запчастям. */
    @PostMapping("/{id}/complete")
    public RepairOrder complete(@PathVariable Long id) {
        return service.complete(id);
    }

    /** Отмена заказа: освобождение всех резервов запчастей. */
    @PostMapping("/{id}/cancel")
    public RepairOrder cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    // Маппинг запроса в сущность (поля, которых нет в запросе, остаются null)
    private RepairOrder toEntity(RepairOrderRequest r) {
        RepairOrder e = new RepairOrder();
        e.setDeviceId(r.deviceId());
        e.setFault(r.fault());
        e.setStatus(r.status());
        e.setCost(r.cost());
        return e;
    }
}
