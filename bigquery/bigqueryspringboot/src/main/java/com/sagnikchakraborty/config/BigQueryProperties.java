package com.sagnikchakraborty.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "bigquery")
public class BigQueryProperties {

    @NotBlank(message = "Dataset ID must not be blank")
    private String datasetId;

    @NotBlank(message = "View name must not be blank")
    private String viewName;

    @Positive(message = "Query timeout must be positive")
    private int queryTimeoutSeconds = 60;

    @Positive(message = "Max results must be positive")
    private long maxResults = 1000;
}
