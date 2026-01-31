package com.sagnikchakraborty.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for BigQuery Insert Query request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigQueryInsertRequestDTO {

    private String region;

    private String country;

    private String itemType;

    private String salesChannel;

    private String orderPriority;

    private LocalDate orderDate;

    private Integer orderId;

    private LocalDate shipDate;

    private Integer unitsSold;

    private Float unitPrice;

    private Float unitCost;

    private Float totalRevenue;

    private Float totalCost;

    private Float totalProfit;
}
