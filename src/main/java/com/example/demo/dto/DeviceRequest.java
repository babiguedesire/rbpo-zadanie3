package com.example.demo.dto;

import com.example.demo.model.*;
import java.math.BigDecimal;

public record DeviceRequest(Long ownerId, String type, String model, String serialNumber) {}
