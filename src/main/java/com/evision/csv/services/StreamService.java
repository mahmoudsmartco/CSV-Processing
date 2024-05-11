package com.evision.csv.services;

import com.evision.csv.config.AppConfig;
import com.evision.csv.kafka.KafkaProducer;
import com.evision.csv.models.CSVFileRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;


@Service
public class StreamService {

    private final AppConfig appConfig;

    public StreamService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    Logger logger = LogManager.getLogger(StreamService.class);
    private static final double EXCHANGE_RATE = 20; // 20 EGP = 1 USD
    private static final double SMALL_AMOUNT_FEE_PERCENTAGE = 0.10;
    private static final double LARGE_AMOUNT_FEE_PERCENTAGE = 0.20;
    @Autowired
    private KafkaProducer kafkaProducer;

    public void streamRecord(CSVFileRecord record) {
        logger.debug("Stream process started");
        try {

            if (record.getRecordStatus().equals("bad")) {
                // Stream to Kafka and store in exception file
                writeToExceptionFile(record);
                kafkaProducer.sendMessage("exception-topic", formatRecordWithFee(record, -1, -1));

            } else {
                double amount = record.getAmount();

                if (amount < 1000) {
                    // Calculate fee for small amounts
                    double fees = amount * SMALL_AMOUNT_FEE_PERCENTAGE;
                    double amountWithFees = amount + fees;
                    // Store in a new file
                    writeToNewFile(record, fees, amountWithFees, "small_amounts_processed.csv");
                    // Send record to Kafka topic for small amounts with fee
                    kafkaProducer.sendMessage("small-amounts-topic", formatRecordWithFee(record, fees, amountWithFees));
                } else {
                    // Convert to USD and calculate fees
                    double amountInUSD = convertToUSD(amount, EXCHANGE_RATE);
                    double fees = amountInUSD * LARGE_AMOUNT_FEE_PERCENTAGE;
                    double amountWithFees = amountInUSD + fees;

                    // Store in a new file
                    writeToNewFile(record, fees, amountWithFees, "large_amounts_processed.csv");
                    // Send record to Kafka topic for large amounts with fee
                    kafkaProducer.sendMessage("large-amounts-topic", formatRecordWithFee(record, fees, amountWithFees));
                }
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        logger.debug("Stream process finished");
    }

    private String formatRecordWithFee(CSVFileRecord record, double fees, double amountWithFees) {
        try {
            return String.format("%s,%s,%.2f,%.2f,%.2f,%s,%s", record.getNationalId(),
                    record.getName(),
                    record.getAmount(),
                    fees,
                    amountWithFees,
                    record.getFileName(),
                    record.getRowNumber());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return "";
        }

    }

    private double convertToUSD(double amountInEGP, double exchangeRate) {
        return amountInEGP / exchangeRate;
    }


    private void writeToNewFile(CSVFileRecord record, double fees, double amountWithFees, String fileName) {
        try {
            String fullFileName = appConfig.getOutputDir() + fileName;
            boolean fileExists = new File(fullFileName).exists();
            FileWriter writer = new FileWriter(fullFileName, true);
            /*if (!fileExists) {
                writer.write("National ID,Name,Amount,Fees,Amount with Fees,fileName,rowNumber,errorMessage\n");
            }*/
            writer.write(String.format("%s,%s,%.2f,%.2f,%.2f,%s,%s,%s\n",
                    record.getNationalId(),
                    record.getName(),
                    record.getAmount(),
                    fees,
                    amountWithFees,
                    record.getFileName(),
                    record.getRowNumber(),
                    record.getErrorMessage()));
            writer.flush();
            writer.close();

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private void writeToExceptionFile(CSVFileRecord record) {

        try {
            String exceptionFileName = appConfig.getOutputDir() + "exception.csv";
            boolean fileExists = new File(exceptionFileName).exists();
            FileWriter writer = new FileWriter(exceptionFileName, true);
            /*if (!fileExists) {
                writer.write("National ID,Name,Amount,fileName,rowNumber\n");
            }*/
            writer.write(String.format("%s,%s,%.2f,%s,%s,%s\n",
                    record.getNationalId(),
                    record.getName(),
                    record.getAmount(),
                    record.getFileName(),
                    record.getRowNumber(),
                    record.getErrorMessage()));
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

    }
}
