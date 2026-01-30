package com.sagnikchakraborty.dto;

import com.sagnikchakraborty.model.IBQQueryRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for BigQuery query responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigQueryResponseDTO {

    private boolean success;

    private String message;

    private List<? extends IBQQueryRecord> data;

    private Long totalRows;

    private String jobId;

    private Long executionTimeMs;
}
