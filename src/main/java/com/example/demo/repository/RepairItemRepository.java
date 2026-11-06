package com.example.demo.repository;

import com.example.demo.model.RepairItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairItemRepository extends JpaRepository<RepairItem, Long> {

    List<RepairItem> findByOrderId(Long orderId);

    List<RepairItem> findBySparePartId(Long sparePartId);
}
