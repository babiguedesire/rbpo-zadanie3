package com.example.demo.dto;

import com.example.demo.model.*;
import java.math.BigDecimal;

public record SparePartRequest(String name, BigDecimal price, Integer stockQuantity, Integer reservedQuantity) {}
