package com.sagnikchakraborty.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BankTransactionRecord implements IBQQueryRecord {

    private LocalDateTime date;

    private String description;

    private Float deposits;

    private Float withdrawal;

    private Float balance;
}
