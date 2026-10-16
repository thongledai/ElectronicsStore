package com.example.store.service.order;

import com.example.store.dto.ghn.CalculateShipmentFeeRequest;
import com.example.store.dto.ghn.ShipmentFeeDTO;

public interface IShipmentService {
    ShipmentFeeDTO calculateFee(CalculateShipmentFeeRequest request);
}