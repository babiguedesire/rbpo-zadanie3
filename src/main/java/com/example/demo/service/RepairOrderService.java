package com.example.demo.service;

import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.Validation;
import com.example.demo.model.ItemStatus;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.RepairItem;
import com.example.demo.model.RepairOrder;
import com.example.demo.model.SparePart;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.RepairItemRepository;
import com.example.demo.repository.RepairOrderRepository;
import com.example.demo.repository.SparePartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class RepairOrderService {

    private final RepairOrderRepository repository;
    private final DeviceRepository deviceRepository;
    private final RepairItemRepository itemRepository;
    private final SparePartRepository partRepository;

    public RepairOrderService(RepairOrderRepository repository, DeviceRepository deviceRepository,
                              RepairItemRepository itemRepository, SparePartRepository partRepository) {
        this.repository = repository;
        this.deviceRepository = deviceRepository;
        this.itemRepository = itemRepository;
        this.partRepository = partRepository;
    }

    // ==================== CRUD ====================

    @Transactional(readOnly = true)
    public List<RepairOrder> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public RepairOrder findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Repair order " + id + " not found"));
    }

    @Transactional
    public RepairOrder create(RepairOrder e) {
        Validation.required(e.getDeviceId(), "deviceId");
        Validation.required(e.getFault(), "fault");
        if (!deviceRepository.existsById(e.getDeviceId())) {
            throw new NotFoundException("Device " + e.getDeviceId() + " not found");
        }
        // Сервер назначает статус, стоимость и дату создания
        e.setStatus(OrderStatus.NEW);
        e.setCost(BigDecimal.ZERO);
        e.setCreatedAt(Instant.now());
        return repository.save(e);
    }

    @Transactional
    public RepairOrder update(Long id, RepairOrder e) {
        RepairOrder existing = findById(id);
        if (e.getFault() != null) existing.setFault(e.getFault());
        if (e.getDeviceId() != null) {
            if (!deviceRepository.existsById(e.getDeviceId())) {
                throw new NotFoundException("Device " + e.getDeviceId() + " not found");
            }
            existing.setDeviceId(e.getDeviceId());
        }
        if (e.getCost() != null) {
            Validation.nonNegative(e.getCost(), "cost");
            existing.setCost(e.getCost());
        }
        // Статус переводится только бизнес-операциями, чтобы не обойти проверку инвариантов
        if (e.getStatus() != null) {
            throw new BadRequestException(
                    "Status is managed by business operations: /start, /wait-for-parts, /resume, /complete, /cancel");
        }
        return repository.save(existing);
    }

    /** Удаление наряда: сначала освобождаются резервы позиций, затем позиции и сам наряд. */
    @Transactional
    public void delete(Long id) {
        RepairOrder order = findById(id);
        for (RepairItem item : itemRepository.findByOrderId(id)) {
            if (item.getUsageStatus() == ItemStatus.RESERVED) {
                releaseReservation(item);
            }
            itemRepository.delete(item);
        }
        repository.delete(order);
    }

    @Transactional
    public void deleteByDeviceId(Long deviceId) {
        for (RepairOrder order : repository.findByDeviceId(deviceId)) {
            delete(order.getId());
        }
    }

    // ==================== Бизнес-операции ====================

    /** 1. Приёмка в работу: NEW -> IN_PROGRESS. */
    @Transactional
    public RepairOrder start(Long id) {
        RepairOrder order = findById(id);
        if (order.getStatus() != OrderStatus.NEW) {
            throw new ConflictException("Order " + id + " cannot be started from status " + order.getStatus());
        }
        order.setStatus(OrderStatus.IN_PROGRESS);
        return repository.save(order);
    }

    /** 2. Ожидание запчастей: IN_PROGRESS -> WAITING_FOR_PARTS (должна быть незакрытая позиция). */
    @Transactional
    public RepairOrder waitForParts(Long id) {
        RepairOrder order = findById(id);
        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new ConflictException("Order " + id + " cannot wait for parts from status " + order.getStatus());
        }
        boolean hasReserved = itemRepository.findByOrderId(id).stream()
                .anyMatch(i -> i.getUsageStatus() == ItemStatus.RESERVED);
        if (!hasReserved) {
            throw new ConflictException("Order " + id + " has no reserved parts to wait for");
        }
        order.setStatus(OrderStatus.WAITING_FOR_PARTS);
        return repository.save(order);
    }

    /** 3. Продолжение работ после получения запчастей: WAITING_FOR_PARTS -> IN_PROGRESS. */
    @Transactional
    public RepairOrder resume(Long id) {
        RepairOrder order = findById(id);
        if (order.getStatus() != OrderStatus.WAITING_FOR_PARTS) {
            throw new ConflictException("Order " + id + " cannot be resumed from status " + order.getStatus());
        }
        order.setStatus(OrderStatus.IN_PROGRESS);
        return repository.save(order);
    }

    /**
     * 4. Завершение ремонта: IN_PROGRESS/WAITING_FOR_PARTS -> COMPLETED.
     * Стоимость = сумма списанных (USED) позиций по зафиксированной цене.
     * Незакрытые резервы запрещают завершение.
     */
    @Transactional
    public RepairOrder complete(Long id) {
        RepairOrder order = findById(id);
        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.WAITING_FOR_PARTS) {
            throw new ConflictException("Order " + id + " cannot be completed from status " + order.getStatus());
        }
        List<RepairItem> items = itemRepository.findByOrderId(id);
        boolean hasReserved = items.stream().anyMatch(i -> i.getUsageStatus() == ItemStatus.RESERVED);
        if (hasReserved) {
            throw new ConflictException("Order " + id + " has reserved parts: use or cancel them before completing");
        }
        BigDecimal cost = items.stream()
                .filter(i -> i.getUsageStatus() == ItemStatus.USED)
                .map(i -> i.getFixedPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setCost(cost);
        order.setStatus(OrderStatus.COMPLETED);
        return repository.save(order);
    }

    /** 5. Отмена заказа: все резервы освобождаются, позиции становятся CANCELLED. */
    @Transactional
    public RepairOrder cancel(Long id) {
        RepairOrder order = findById(id);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Order " + id + " cannot be cancelled from status " + order.getStatus());
        }
        for (RepairItem item : itemRepository.findByOrderId(id)) {
            if (item.getUsageStatus() == ItemStatus.RESERVED) {
                item.setUsageStatus(ItemStatus.CANCELLED);
                releaseReservation(item);
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        return repository.save(order);
    }

    /** Освобождение резерва: reserved_quantity запчасти уменьшается на количество позиции. */
    private void releaseReservation(RepairItem item) {
        SparePart part = partRepository.findByIdForUpdate(item.getSparePartId())
                .orElseThrow(() -> new NotFoundException("Spare part " + item.getSparePartId() + " not found"));
        part.setReservedQuantity(part.getReservedQuantity() - item.getQuantity());
        partRepository.save(part);
    }
}
