package com.sagnikchakraborty.service;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.sagnikchakraborty.config.BigQueryProperties;
import com.sagnikchakraborty.dto.BigQueryInsertRequestDTO;
import com.sagnikchakraborty.dto.BigQueryResponseDTO;
import com.sagnikchakraborty.exception.BigQueryException;
import com.sagnikchakraborty.model.BankTransactionRecord;
import com.sagnikchakraborty.model.SalesRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigQueryService {

    private final BigQuery bigQuery;
    private final BigQueryProperties properties;

    /**
     * Read data from a BigQuery view with no filters applied
     * @param viewName BigQuery view name
     * @return BigQueryResponseDTO
     */
    public BigQueryResponseDTO readFromView(String viewName) {
        return readFromView(viewName, null);
    }

    /**
     * Read data from a BigQuery view
     * @param viewName BigQuery view name
     * @param filters Filters to apply in the query
     * @return BigQueryResponseDTO
     */
    public BigQueryResponseDTO readFromView(String viewName, Map<String, Object> filters) {
        long startTime = System.currentTimeMillis();

        try {
            String query = buildQuery(viewName, filters);
            log.info("Executing BigQuery View: {}", query);

            QueryJobConfiguration queryConfig = QueryJobConfiguration
                    .newBuilder(query)
                    .setUseLegacySql(false)
                    .build();

            // Fire the query...
            TableResult tableResult = bigQuery.query(queryConfig);

            // Parse result data and return response...
            List<SalesRecord> rows = parseViewResults(tableResult);
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Query executed successfully. Rows: {}, Time: {}ms",
                    rows.size(), executionTime);

            return new BigQueryResponseDTO(
                    true,
                    "Data retrieved successfully",
                    rows,
                    tableResult.getTotalRows(),
                    tableResult.getJobId() == null ? null : tableResult.getJobId().getJob(),
                    executionTime
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BigQueryException("Query was interrupted", e);
        } catch (Exception e) {
            log.error("Error executing BigQuery", e);
            throw new BigQueryException("Failed to read from BigQuery view", e);
        }
    }

    /**
     * Read data from a BigQuery table with no filters applied
     * @param tableName BigQuery table name
     * @return BigQueryResponseDTO
     */
    public BigQueryResponseDTO readFromTable(String tableName) {
        return readFromTable(tableName, null);
    }

    /**
     * Read data from a BigQuery table
     * @param tableName BigQuery table name
     * @param filters Filters to apply in the query
     * @return BigQueryResponseDTO
     */
    public BigQueryResponseDTO readFromTable(String tableName, Map<String, Object> filters) {
        long startTime = System.currentTimeMillis();

        try {
            String query = buildQuery(tableName, filters);
            log.info("Executing BigQuery Table: {}", query);

            QueryJobConfiguration queryConfig = QueryJobConfiguration
                    .newBuilder(query)
                    .setUseLegacySql(false)
                    .build();

            // Fire the query...
            TableResult tableResult = bigQuery.query(queryConfig);

            // Parse result data and return response...
            List<BankTransactionRecord> rows = parseTableResults(tableResult);
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Query executed successfully. Rows: {}, Time: {}ms",
                    rows.size(), executionTime);

            return new BigQueryResponseDTO(
                    true,
                    "Data retrieved successfully",
                    rows,
                    tableResult.getTotalRows(),
                    tableResult.getJobId() == null ? null : tableResult.getJobId().getJob(),
                    executionTime
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BigQueryException("Query was interrupted", e);
        } catch (Exception e) {
            log.error("Error executing BigQuery", e);
            throw new BigQueryException("Failed to read from BigQuery view", e);
        }
    }

    private String buildQuery(String tableName, Map<String, Object> filters) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("SELECT * FROM `%s.%s.%s`",
                bigQuery.getOptions().getProjectId(),
                properties.getDatasetId(),
                tableName));

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

    private List<SalesRecord> parseViewResults(TableResult tableResult) {
        List<SalesRecord> tableRows = new ArrayList<>();

        for (FieldValueList row : tableResult.iterateAll()) {
            SalesRecord record = new SalesRecord();

            record.setRegion(
                    row.get("region").isNull() ? null : row.get("region").getStringValue());
            record.setCountry(
                    row.get("country").isNull() ? null : row.get("country").getStringValue());
            record.setItemType(
                    row.get("item_type").isNull() ? null : row.get("item_type").getStringValue());
            record.setSalesChannel(
                    row.get("sales_channel").isNull() ? null : row.get("sales_channel").getStringValue());
            record.setOrderPriority(
                    row.get("order_priority").isNull() ? null : row.get("order_priority").getStringValue());
            record.setOrderDate(
                    row.get("order_date").isNull() ? null : LocalDateTime.parse(row.get("order_date").getStringValue()));
            record.setOrderId(
                    row.get("order_id").isNull() ? null : row.get("order_id").getNumericValue().intValue());
            record.setShipDate(
                    row.get("ship_date").isNull() ? null : LocalDateTime.parse(row.get("ship_date").getStringValue()));
            record.setUnitsSold(
                    row.get("units_sold").isNull() ? null : row.get("units_sold").getNumericValue().intValue());
            record.setUnitPrice(
                    row.get("unit_price").isNull()? null : row.get("unit_price").getNumericValue().floatValue());
            record.setUnitCost(
                    row.get("unit_cost").isNull() ? null : row.get("unit_cost").getNumericValue().floatValue());
            record.setTotalRevenue(
                    row.get("total_revenue").isNull() ? null : row.get("total_revenue").getNumericValue().floatValue());
            record.setTotalCost(
                    row.get("total_cost").isNull() ? null : row.get("total_cost").getNumericValue().floatValue());
            record.setTotalProfit(
                    row.get("total_profit").isNull() ? null : row.get("total_profit").getNumericValue().floatValue());

            tableRows.add(record);
        }

        return tableRows;
    }

    private List<BankTransactionRecord> parseTableResults(TableResult tableResult) {
        List<BankTransactionRecord> tableRows = new ArrayList<>();

        for (FieldValueList row : tableResult.iterateAll()) {
            BankTransactionRecord record = new BankTransactionRecord();

            record.setDate(
                    row.get("date").isNull() ? null : LocalDate.parse(row.get("date").getStringValue()).atStartOfDay());
            record.setDescription(
                    row.get("description").isNull() ? null : row.get("description").getStringValue());
            record.setDeposits(
                    row.get("deposit").isNull() ? null : row.get("deposit").getNumericValue().floatValue());
            record.setWithdrawal(
                    row.get("withdrawal").isNull() ? null : row.get("withdrawal").getNumericValue().floatValue());
            record.setBalance(
                    row.get("balance").isNull() ? null : row.get("balance").getNumericValue().floatValue());

            tableRows.add(record);
        }

        return tableRows;
    }

    private String sanitizeValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        return value.toString().replace("'", "\\'");
    }

    /**
     * Insert a single SalesRecord into a BigQuery table using SQL INSERT query.
     * This method is simpler than streaming insert and suitable for single record inserts.
     * @param tableName BigQuery table name
     * @param requestDTO Single record to insert
     * @return BigQueryResponseDTO with insert results
     */
    public BigQueryResponseDTO insertIntoTable(String tableName, BigQueryInsertRequestDTO requestDTO) {
        long startTime = System.currentTimeMillis();
        try {
            if (requestDTO == null) {
                return new BigQueryResponseDTO(
                        false,
                        "No record provided to insert",
                        null,
                        0L,
                        null,
                        0L);
            }

            // Build the INSERT SQL query
            String insertQuery = buildInsertQuery(tableName, requestDTO);
            log.info("Executing INSERT query for table: {}", tableName);

            QueryJobConfiguration queryConfig = QueryJobConfiguration
                    .newBuilder(insertQuery)
                    .setUseLegacySql(false)
                    .build();

            // Execute the insert query
            TableResult tableResult = bigQuery.query(queryConfig);
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Record inserted successfully into {} in {}ms", tableName, executionTime);
            return new BigQueryResponseDTO(
                    true,
                    "Record inserted successfully",
                    null,
                    1L,
                    tableResult.getJobId() == null ? null : tableResult.getJobId().getJob(),
                    executionTime);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Insert operation interrupted", e);
            throw new BigQueryException("Insert operation was interrupted", e);
        } catch (Exception e) {
            log.error("Error inserting into BigQuery", e);
            throw new BigQueryException("Failed to insert into BigQuery table", e);
        }
    }

    /**
     * Build an INSERT SQL query for the given DTO.
     * @param tableName BigQuery table name
     * @param dto the data transfer object
     * @return INSERT SQL query string
     */
    private String buildInsertQuery(String tableName, BigQueryInsertRequestDTO dto) {
        String fullTableName = String.format("`%s.%s.%s`",
                bigQuery.getOptions().getProjectId(),
                properties.getDatasetId(),
                tableName);

        return String.format(
                "INSERT INTO %s (region, country, item_type, sales_channel, order_priority, order_date, order_id, " +
                "ship_date, units_sold, unit_price, unit_cost, total_revenue, total_cost, total_profit) " +
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                fullTableName,
                formatSQLValue(dto.getRegion()),
                formatSQLValue(dto.getCountry()),
                formatSQLValue(dto.getItemType()),
                formatSQLValue(dto.getSalesChannel()),
                formatSQLValue(dto.getOrderPriority()),
                formatSQLValue(dto.getOrderDate() == null ? null : dto.getOrderDate().toString()),
                formatSQLValue(dto.getOrderId()),
                formatSQLValue(dto.getShipDate() == null ? null : dto.getShipDate().toString()),
                formatSQLValue(dto.getUnitsSold()),
                formatSQLValue(dto.getUnitPrice()),
                formatSQLValue(dto.getUnitCost()),
                formatSQLValue(dto.getTotalRevenue()),
                formatSQLValue(dto.getTotalCost()),
                formatSQLValue(dto.getTotalProfit())
        );
    }

    /**
     * Format a value for SQL query (handle nulls, strings, and numbers).
     * @param value the value to format
     * @return SQL formatted value
     */
    private String formatSQLValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String stringValue) {
            // Escape single quotes for SQL
            return "'" + stringValue.replace("'", "\\'") + "'";
        }
        // Numbers and other types can be used directly
        return value.toString();
    }
}
