package com.sagnikchakraborty.service;

import com.google.cloud.bigquery.*;
import com.sagnikchakraborty.config.BigQueryProperties;
import com.sagnikchakraborty.dto.BigQueryResponseDTO;
import com.sagnikchakraborty.exception.BigQueryException;
import com.sagnikchakraborty.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigQueryService {

    private final BigQuery bigQuery;
    private final BigQueryProperties properties;

    @Value("${spring.cloud.gcp.bigquery.datasetName}")
    private String datasetName;

    /**
     * Read data from a BigQuery table
     * @return BigQueryResponseDTO
     */
    public BigQueryResponseDTO readTable(String tableName) {
        try {
            long startTime = System.currentTimeMillis();

            String sql = buildSelectQuery(tableName);
            log.info("Executing query: {}", sql);

            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(sql).build();
            Job queryJob = bigQuery.create(JobInfo.newBuilder(queryConfig).build());

            // Wait for the query to complete...
            queryJob = queryJob.waitFor();

            if (queryJob == null) {
                throw new RuntimeException("Job no longer exists");
            } else if (queryJob.getStatus().getError() != null) {
                throw new RuntimeException(queryJob.getStatus().getError().toString());
            }

            TableResult result = queryJob.getQueryResults();
            List<Map<String, Object>> rows = new ArrayList<>();

            for (FieldValueList row : result.iterateAll()) {
                Map<String, Object> rowMap = new HashMap<>();
                for (Field field : result.getSchema().getFields()) {
                    FieldValue fieldValue = row.get(field.getName());
                    rowMap.put(field.getName(), getFieldValue(fieldValue));
                }
                rows.add(rowMap);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            return new BigQueryResponseDTO(
                    true,
                    "Data retrieved successfully",
                    rows,
                    result.getTotalRows(),
                    queryJob.getJobId().getJob(),
                    executionTime);

        } catch (Exception e) {
            log.error("Error reading from table: {}", e.getMessage(), e);
            return new BigQueryResponseDTO(
                    false,
                    "Error: " + e.getMessage(),
                    null,
                    0L,
                    null,
                    0L);
        }
    }

    /**
     * Read data from a BigQuery view
     * @return BigQueryResponseDTO
     */
    public BigQueryResponseDTO readView(String viewName) {
        // Views are queried the same way as tables in BigQuery
        return readTable(viewName);
    }

    private String buildSelectQuery(String tableName) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT * FROM `")
                .append(datasetName)
                .append(".")
                .append(tableName)
                .append("` LIMIT 100");

        return sql.toString();
    }

    private Object getFieldValue(FieldValue fieldValue) {
        if (fieldValue.isNull()) {
            return null;
        }

        switch (fieldValue.getAttribute()) {
            case PRIMITIVE:
                return fieldValue.getValue();
            case REPEATED:
                List<Object> list = new ArrayList<>();
                for (FieldValue item : fieldValue.getRepeatedValue()) {
                    list.add(getFieldValue(item));
                }
                return list;
            case RECORD:
                Map<String, Object> record = new HashMap<>();
                for (FieldValue subField : fieldValue.getRecordValue()) {
                    // This is not a production level approach, we would need the schema
                    // to get the field names properly
                    record.put(subField.toString(), getFieldValue(subField));
                }
                return record;
            default:
                return fieldValue.getStringValue();
        }
    }

    public QueryResult readFromView() {
        return readFromView(null);
    }

    public QueryResult readFromView(Map<String, Object> filters) {
        long startTime = System.currentTimeMillis();

        try {
            String query = buildQuery(filters);
            log.info("Executing BigQuery: {}", query);

            QueryJobConfiguration queryConfig = QueryJobConfiguration
                    .newBuilder(query)
                    .setUseLegacySql(false)
                    .build();

            TableResult result = bigQuery.query(queryConfig);
            log.info(String.valueOf(result));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BigQueryException("Query was interrupted", e);
        } catch (Exception e) {
            log.error("Error executing BigQuery", e);
            throw new BigQueryException("Failed to read from BigQuery view", e);
        }

        return null;
    }

    private String buildQuery(Map<String, Object> filters) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("SELECT * FROM `%s.%s.%s`",
                bigQuery.getOptions().getProjectId(),
                properties.getDatasetId(),
                properties.getViewName()));

        if (filters != null && !filters.isEmpty()) {
            query.append("WHERE ");
            String whereClause = filters.entrySet().stream()
                    .map(entry -> String.format("%s = '%s'",
                            entry.getKey(),
                            sanitizeValue(entry.getValue())))
                    .collect(Collectors.joining(" AND "));
            query.append(whereClause);
        }

        query.append(String.format(" LIMIT %d", properties.getMaxResults()));

        return query.toString();
    }

//    private List<Map<String, Object>> parseResults(TableResult result) {
//        return StreamSupport.stream(result.iterateAll().spliterator(), false)
//                .map()
//    }

//    private Map<String, Object> rowToMap(FieldValueList row) {
//        Map<String, Object> map = new LinkedHashMap<>();
//        row.forEach(field -> {
//            String fieldName = field.getName();
//        });
//        return map;
//    }

//    private Object extractFieldValue(FieldValue field) {
//        if (field.isNull()) {
//            return null;
//        }
//
//        FieldValue.Attribute attribute = field.getAttribute();
//
//        return switch (attribute) {
//            case PRIMITIVE -> field.getValue();
//            case REPEATED -> field.getRepeatedValue().stream()
//                    .map(this::extractFieldValue)
//                    .collect(Collectors.toList());
//            case RECORD -> {
//                FieldValueList recordvalue = field.getRecordValue();
//                Map<String, Object> recordMap = new LinkedHashMap<>();
//                Schema recordSchema = recordvalue.getSchema();
//            }
//        };
//    }

    private String sanitizeValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        return value.toString().replace("'", "\\'");
    }
}
