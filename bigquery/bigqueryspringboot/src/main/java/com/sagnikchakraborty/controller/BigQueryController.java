package com.sagnikchakraborty.controller;

import com.sagnikchakraborty.dto.BigQueryInsertRequestDTO;
import com.sagnikchakraborty.dto.BigQueryResponseDTO;
import com.sagnikchakraborty.service.BigQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/bigquery")
@RequiredArgsConstructor
public class BigQueryController {

    private final BigQueryService bigQueryService;

    @GetMapping("/view/{viewName}")
    public ResponseEntity<BigQueryResponseDTO> readFromView(@PathVariable String viewName) {
        BigQueryResponseDTO response = bigQueryService.readFromView(viewName);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @GetMapping("/table/{tableName}")
    public ResponseEntity<BigQueryResponseDTO> readFromTable(@PathVariable String tableName) {
        BigQueryResponseDTO response = bigQueryService.readFromTable(tableName);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @PostMapping("/table/{tableName}")
    public ResponseEntity<BigQueryResponseDTO> insertIntoTable(@PathVariable String tableName,
                                                               @RequestBody BigQueryInsertRequestDTO requestDTO) {
        BigQueryResponseDTO response = bigQueryService.insertIntoTable(tableName, requestDTO);
        return response.isSuccess()
                ? ResponseEntity.status(HttpStatus.CREATED).body(response)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
