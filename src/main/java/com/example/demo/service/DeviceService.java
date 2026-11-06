package com.example.demo.service;

import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.Validation;
import com.example.demo.model.Device;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository repository;
    private final UserRepository userRepository;
    private final RepairOrderService orderService;

    public DeviceService(DeviceRepository repository, UserRepository userRepository, RepairOrderService orderService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public List<Device> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Device findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Device " + id + " not found"));
    }

    @Transactional
    public Device create(Device e) {
        Validation.required(e.getOwnerId(), "ownerId");
        Validation.required(e.getType(), "type");
        Validation.required(e.getModel(), "model");
        Validation.required(e.getSerialNumber(), "serialNumber");
        if (!userRepository.existsById(e.getOwnerId())) {
            throw new NotFoundException("User " + e.getOwnerId() + " not found");
        }
        if (repository.findBySerialNumber(e.getSerialNumber()).isPresent()) {
            throw new ConflictException("Serial number '" + e.getSerialNumber() + "' already exists");
        }
        return repository.save(e);
    }

    @Transactional
    public Device update(Long id, Device e) {
        Device existing = findById(id);
        if (e.getOwnerId() != null) {
            if (!userRepository.existsById(e.getOwnerId())) {
                throw new NotFoundException("User " + e.getOwnerId() + " not found");
            }
            existing.setOwnerId(e.getOwnerId());
        }
        if (e.getType() != null) existing.setType(e.getType());
        if (e.getModel() != null) existing.setModel(e.getModel());
        if (e.getSerialNumber() != null) {
            repository.findBySerialNumber(e.getSerialNumber())
                    .filter(d -> !id.equals(d.getId()))
                    .ifPresent(d -> {
                        throw new ConflictException("Serial number '" + e.getSerialNumber() + "' already exists");
                    });
            existing.setSerialNumber(e.getSerialNumber());
        }
        return repository.save(existing);
    }

    /** Каскадное удаление: устройство -> наряды (вместе с позициями). */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Device " + id + " not found");
        }
        orderService.deleteByDeviceId(id);
        repository.deleteById(id);
    }

    @Transactional
    public void deleteByOwnerId(Long ownerId) {
        for (Device d : repository.findByOwnerId(ownerId)) {
            delete(d.getId());
        }
    }
}
