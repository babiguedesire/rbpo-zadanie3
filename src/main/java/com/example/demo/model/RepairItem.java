package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Позиция наряда: запчасть, зарезервированная или списанная для ремонта. */
@Entity
@Table(name = "repair_items")
public class RepairItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "spare_part_id", nullable = false)
    private Long sparePartId;

    @Column(nullable = false)
    private Integer quantity;

    /** Цена фиксируется в момент добавления позиции. */
    @Column(name = "fixed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_status", nullable = false, length = 20)
    private ItemStatus usageStatus = ItemStatus.RESERVED;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getSparePartId() { return sparePartId; }
    public void setSparePartId(Long sparePartId) { this.sparePartId = sparePartId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getFixedPrice() { return fixedPrice; }
    public void setFixedPrice(BigDecimal fixedPrice) { this.fixedPrice = fixedPrice; }

    public ItemStatus getUsageStatus() { return usageStatus; }
    public void setUsageStatus(ItemStatus usageStatus) { this.usageStatus = usageStatus; }
}
