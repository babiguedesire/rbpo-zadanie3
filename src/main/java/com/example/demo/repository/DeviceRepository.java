package com.example.demo.repository;

import com.example.demo.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByOwnerId(Long ownerId);

    Optional<Device> findBySerialNumber(String serialNumber);
}
