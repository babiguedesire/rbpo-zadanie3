package com.example.demo.service;

import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.Validation;
import com.example.demo.model.RepairItem;
import com.example.demo.model.SparePart;
import com.example.demo.repository.RepairItemRepository;
import com.example.demo.repository.SparePartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SparePartService {

    private final SparePartRepository repository;
    private final RepairItemRepository itemRepository;

    public SparePartService(SparePartRepository repository, RepairItemRepository itemRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<SparePart> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public SparePart findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Spare part " + id + " not found"));
    }

    @Transactional
    public SparePart create(SparePart e) {
        Validation.required(e.getName(), "name");
        Validation.required(e.getPrice(), "price");
        Validation.nonNegative(e.getPrice(), "price");
        Validation.nonNegative(e.getStockQuantity(), "stockQuantity");
        Validation.nonNegative(e.getReservedQuantity(), "reservedQuantity");
        if (e.getStockQuantity() == null) e.setStockQuantity(0);
        if (e.getReservedQuantity() == null) e.setReservedQuantity(0);
        if (e.getReservedQuantity() > e.getStockQuantity()) {
            throw new ConflictException("reservedQuantity must not be greater than stockQuantity");
        }
        return repository.save(e);
    }

    @Transactional
    public SparePart update(Long id, SparePart e) {
        SparePart existing = findById(id);
        if (e.getName() != null) existing.setName(e.getName());
        if (e.getPrice() != null) {
            Validation.nonNegative(e.getPrice(), "price");
            existing.setPrice(e.getPrice());
        }
        if (e.getStockQuantity() != null) {
            Validation.nonNegative(e.getStockQuantity(), "stockQuantity");
            existing.setStockQuantity(e.getStockQuantity());
        }
        // Резерв управляется только бизнес-операциями (резервирование/списание позиций)
        if (e.getReservedQuantity() != null) {
            throw new BadRequestException("reservedQuantity is managed by the system (item reservation)");
        }
        if (existing.getReservedQuantity() > existing.getStockQuantity()) {
            throw new ConflictException("reservedQuantity must not be greater than stockQuantity");
        }
        return repository.save(existing);
    }

    /** Удаление запчасти вместе с её позициями в нарядах. */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Spare part " + id + " not found");
        }
        for (RepairItem item : itemRepository.findBySparePartId(id)) {
            itemRepository.delete(item);
        }
        repository.deleteById(id);
    }
}
