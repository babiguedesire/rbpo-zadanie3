package com.example.demo.dto;

import com.example.demo.model.*;
import java.math.BigDecimal;

public record RepairOrderRequest(Long deviceId, String fault, OrderStatus status, BigDecimal cost) {}
