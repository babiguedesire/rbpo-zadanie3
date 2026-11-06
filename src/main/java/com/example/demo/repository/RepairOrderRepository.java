package com.example.demo.repository;

import com.example.demo.model.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    List<RepairOrder> findByDeviceId(Long deviceId);
}
