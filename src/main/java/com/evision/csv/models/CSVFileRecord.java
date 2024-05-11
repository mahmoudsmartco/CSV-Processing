package com.evision.csv.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CSVFileRecord {
    private String nationalId;
    private String name;
    private double amount;
    private String fileName;
    private long rowNumber;
    private String recordStatus;
    private String errorMessage;

}
