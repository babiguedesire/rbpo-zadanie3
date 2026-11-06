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
import com.example.demo.repository.RepairItemRepository;
import com.example.demo.repository.RepairOrderRepository;
import com.example.demo.repository.SparePartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RepairItemService {

    private final RepairItemRepository repository;
    private final RepairOrderRepository orderRepository;
    private final SparePartRepository partRepository;

    public RepairItemService(RepairItemRepository repository, RepairOrderRepository orderRepository,
                             SparePartRepository partRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
    }

    @Transactional(readOnly = true)
    public List<RepairItem> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public RepairItem findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Repair item " + id + " not found"));
    }

    /**
     * 6. Бизнес-операция: добавить запчасть в наряд с резервированием на складе.
     * Проверяет активность наряда и наличие свободного остатка, фиксирует цену
     * и увеличивает reserved_quantity запчасти. Позиция создаётся со статусом RESERVED.
     */
    @Transactional
    public RepairItem create(RepairItem e) {
        Validation.required(e.getOrderId(), "orderId");
        Validation.required(e.getSparePartId(), "sparePartId");
        Validation.required(e.getQuantity(), "quantity");
        if (e.getQuantity() <= 0) {
            throw new BadRequestException("Field 'quantity' must be positive");
        }
        Validation.nonNegative(e.getFixedPrice(), "fixedPrice");

        RepairOrder order = orderRepository.findById(e.getOrderId())
                .orElseThrow(() -> new NotFoundException("Repair order " + e.getOrderId() + " not found"));
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Order " + e.getOrderId() + " is " + order.getStatus()
                    + " and cannot accept parts");
        }

        SparePart part = partRepository.findByIdForUpdate(e.getSparePartId())
                .orElseThrow(() -> new NotFoundException("Spare part " + e.getSparePartId() + " not found"));
        int free = part.getStockQuantity() - part.getReservedQuantity();
        if (e.getQuantity() > free) {
            throw new ConflictException("Not enough stock for part " + e.getSparePartId()
                    + ": free " + free + ", requested " + e.getQuantity());
        }

        // Цена фиксируется в момент добавления позиции
        if (e.getFixedPrice() == null) {
            e.setFixedPrice(part.getPrice());
        }
        part.setReservedQuantity(part.getReservedQuantity() + e.getQuantity());
        partRepository.save(part);

        e.setUsageStatus(ItemStatus.RESERVED);
        return repository.save(e);
    }

    /** 7. Бизнес-операция: списание запчасти со склада (RESERVED -> USED). */
    @Transactional
    public RepairItem use(Long id) {
        RepairItem item = findById(id);
        if (item.getUsageStatus() != ItemStatus.RESERVED) {
            throw new ConflictException("Item " + id + " is " + item.getUsageStatus() + " and cannot be used");
        }
        SparePart part = partRepository.findByIdForUpdate(item.getSparePartId())
                .orElseThrow(() -> new NotFoundException("Spare part " + item.getSparePartId() + " not found"));
        // Уменьшаем и склад, и резерв — запчасть ушла в ремонт
        part.setReservedQuantity(part.getReservedQuantity() - item.getQuantity());
        part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
        partRepository.save(part);

        item.setUsageStatus(ItemStatus.USED);
        return repository.save(item);
    }

    /** 8. Бизнес-операция: отмена позиции с возвратом резерва (RESERVED -> CANCELLED). */
    @Transactional
    public RepairItem cancel(Long id) {
        RepairItem item = findById(id);
        if (item.getUsageStatus() != ItemStatus.RESERVED) {
            throw new ConflictException("Item " + id + " is " + item.getUsageStatus() + " and cannot be cancelled");
        }
        releaseReservation(item);
        item.setUsageStatus(ItemStatus.CANCELLED);
        return repository.save(item);
    }

    @Transactional
    public RepairItem update(Long id, RepairItem e) {
        RepairItem existing = findById(id);
        // Статус использования переводится только бизнес-операциями /use и /cancel
        if (e.getUsageStatus() != null) {
            throw new BadRequestException("usageStatus is managed by business operations: /use and /cancel");
        }
        if (e.getQuantity() != null) {
            if (existing.getUsageStatus() != ItemStatus.RESERVED) {
                throw new ConflictException("Only reserved items can change quantity");
            }
            if (e.getQuantity() <= 0) {
                throw new BadRequestException("Field 'quantity' must be positive");
            }
            SparePart part = partRepository.findByIdForUpdate(existing.getSparePartId())
                    .orElseThrow(() -> new NotFoundException("Spare part " + existing.getSparePartId() + " not found"));
            int delta = e.getQuantity() - existing.getQuantity();
            int free = part.getStockQuantity() - part.getReservedQuantity();
            if (delta > free) {
                throw new ConflictException("Not enough stock for part " + existing.getSparePartId()
                        + ": free " + free + ", requested change +" + delta);
            }
            part.setReservedQuantity(part.getReservedQuantity() + delta);
            partRepository.save(part);
            existing.setQuantity(e.getQuantity());
        }
        if (e.getFixedPrice() != null) {
            Validation.nonNegative(e.getFixedPrice(), "fixedPrice");
            existing.setFixedPrice(e.getFixedPrice());
        }
        return repository.save(existing);
    }

    /** Удаление позиции: зарезервированная позиция сначала возвращает резерв на склад. */
    @Transactional
    public void delete(Long id) {
        RepairItem item = findById(id);
        if (item.getUsageStatus() == ItemStatus.RESERVED) {
            releaseReservation(item);
        }
        repository.delete(item);
    }

    private void releaseReservation(RepairItem item) {
        SparePart part = partRepository.findByIdForUpdate(item.getSparePartId())
                .orElseThrow(() -> new NotFoundException("Spare part " + item.getSparePartId() + " not found"));
        part.setReservedQuantity(part.getReservedQuantity() - item.getQuantity());
        partRepository.save(part);
    }
}
