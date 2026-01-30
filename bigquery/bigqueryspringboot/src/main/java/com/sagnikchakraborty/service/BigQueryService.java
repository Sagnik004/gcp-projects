package com.sagnikchakraborty.service;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.sagnikchakraborty.config.BigQueryProperties;
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
}
