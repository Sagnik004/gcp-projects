package com.sagnikchakraborty.controller;

import com.sagnikchakraborty.dto.BigQueryResponseDTO;
import com.sagnikchakraborty.service.BigQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/bigquery")
@RequiredArgsConstructor
public class BigQueryController {

    private final BigQueryService bigQueryService;

    @GetMapping("/view/{viewName}")
    public ResponseEntity<BigQueryResponseDTO> readView(@PathVariable String viewName) {
        BigQueryResponseDTO response = bigQueryService.readFromView(viewName);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
