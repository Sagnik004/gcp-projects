package com.sagnikchakraborty.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for BigQuery query responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigQueryResponseDTO {

    private boolean success;

    private String message;

    private List<Map<String, Object>> data;

    private Long totalRows;

    private String jobId;

    private Long executionTimeMs;
}
