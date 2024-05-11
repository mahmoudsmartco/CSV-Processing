package com.evision.csv.controller;

import com.evision.csv.dto.SFTPLoginDTO;
import com.evision.csv.services.CSVService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/csv")
public class CSVController {


    @Autowired
    private CSVService csvParserService;
    Logger logger = LogManager.getLogger(CSVController.class);

    @PostMapping("/process")
    public String processCSVFiles(@RequestBody List<SFTPLoginDTO> sftpLoginDTOList) {
        logger.info("REST API request to process CSV files started ");
        try {
        logger.debug("user input: \n" + sftpLoginDTOList);
            // Parse CSV file and process records
            csvParserService.process(sftpLoginDTOList);

            return "CSV files transferred and processed successfully. for more information check application.log";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred while transfer or processing the CSV files. for more information check application.log";
        }
        finally {
            logger.info("REST API request to process CSV files finished");
        }

    }
}