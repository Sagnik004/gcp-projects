package com.sagnikchakraborty.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {

    private List<SalesRecord> rows;

    private long totalRows;

    private String schema;

    private long executionTimeMs;
}
