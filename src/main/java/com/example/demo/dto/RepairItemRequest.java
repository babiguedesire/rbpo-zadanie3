package com.example.demo.dto;

import com.example.demo.model.*;
import java.math.BigDecimal;

public record RepairItemRequest(Long orderId, Long sparePartId, Integer quantity, BigDecimal fixedPrice, ItemStatus usageStatus) {}
