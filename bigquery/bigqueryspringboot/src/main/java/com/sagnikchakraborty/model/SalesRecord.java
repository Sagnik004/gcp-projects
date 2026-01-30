package com.sagnikchakraborty.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalesRecord implements IBQQueryRecord{

    private String region;
    private String country;
    private String itemType;
    private String salesChannel;
    private String orderPriority;
    private LocalDateTime orderDate;
    private Integer orderId;
    private LocalDateTime shipDate;
    private Integer unitsSold;
    private Float unitPrice;
    private Float unitCost;
    private Float totalRevenue;
    private Float totalCost;
    private Float totalProfit;
}
