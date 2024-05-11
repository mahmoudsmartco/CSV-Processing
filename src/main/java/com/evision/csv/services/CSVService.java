package com.evision.csv.services;

import com.evision.csv.collector.sftp.SFTPFunctions;
import com.evision.csv.config.AppConfig;
import com.evision.csv.dto.SFTPLoginDTO;
import com.evision.csv.models.CSVFileRecord;
import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class CSVService {

    @Autowired
    private StreamService streamService;
    private final AppConfig appConfig;

    public CSVService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    Logger logger = LogManager.getLogger(CSVService.class);


    public void process(List<SFTPLoginDTO> sftpLoginDTOList) {

        ExecutorService executor = null;
        try {
            //call Collector to transfer csv files from SFTP to the local path
            // download throw SFTP
            logger.info("Downloading Process Started");
            // collectors instances
            SFTPFunctions sftpFunctions = new SFTPFunctions();

            // loop on SFTP DTOs
            for (int i = 0; i < sftpLoginDTOList.size(); i++) {
                SFTPLoginDTO sftpLoginDTO = sftpLoginDTOList.get(i);
                ChannelSftp channelSftp = sftpFunctions.connectToSFTP(sftpLoginDTO.getServerIp(), Integer.parseInt(sftpLoginDTO.getServerPort()), sftpLoginDTO.getServerUser(), sftpLoginDTO.getServerPass(), sftpLoginDTO.getSshKeyPath(), sftpLoginDTO.getSshKeyPass());
                if (channelSftp != null) {
                    sftpFunctions.downloadFile(channelSftp, sftpLoginDTO.getDirectoryPath(), "*", appConfig.getLocalPath(), null, "END_WITH", ".csv", "DELETE");
                }
            }

            logger.info("Downloading Process finished");
            // files are ready in the local path


            List<Callable<Void>> tasks = new ArrayList<>();
            // loop on files
            File directory = new File(appConfig.getLocalPath());
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            logger.info("The count of downloaded files is : " + files.length);
            if (files != null) {
                for (File file : files) {
                    // Execute parsing of each CSV file in a separate thread
                    tasks.add(() -> {
                        parseCSVFile(file.getPath().toLowerCase());
                        return null; // Callable<Void> requires returning null
                    });
                }
            }

            executor = Executors.newFixedThreadPool(Integer.parseInt(appConfig.getThreadNumbers()));

            // Execute all tasks concurrently and get their futures
            List<Future<Void>> futures = executor.invokeAll(tasks);

            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get(); // Wait for completion
            }


        } catch (Exception ex) {
            logger.error(ex.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    public void parseCSVFile(String filePath) {
        logger.info("Parse CSV File process started for file name : " + filePath);
        CSVFileRecord csvFileRecord;
        String currentFileName = "";
        long timeStamp = 0;
        try {

            timeStamp = Instant.now().getEpochSecond();
            currentFileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
            int i = 0;
            String nationalId = "";
            String name = "";
            double amount = -1;
            Reader reader = null;
            CSVParser csvParser = null;
            try {
                reader = new FileReader(filePath);
                csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
                int rowsCount = 0;


                for (CSVRecord csvRecord : csvParser) {
                    try {
                        rowsCount++;
                        i++;
                        nationalId = csvRecord.get("National ID");
                        name = csvRecord.get("Name");
                        amount = Double.parseDouble(csvRecord.get("Amount"));
                        csvFileRecord = new CSVFileRecord(nationalId, name, amount, currentFileName + "__" + timeStamp, i, "ok", "");
                        // stream
                        streamService.streamRecord(csvFileRecord);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage());
                        csvFileRecord = new CSVFileRecord(nationalId, name, amount, currentFileName + "__" + timeStamp, i, "bad", "wrong data : " + ex.getMessage());
                        // stream
                        streamService.streamRecord(csvFileRecord);
                    }

                }
                if (rowsCount < 1) {
                    logger.info("file:  " + filePath + " is empty file.");
                    csvFileRecord = new CSVFileRecord("", "", -1, currentFileName + "__" + timeStamp, 0, "bad", "Empty File ");
                    // stream
                    streamService.streamRecord(csvFileRecord);
                }

            } catch (Exception ex) {
                logger.error(ex.getMessage());
                csvFileRecord = new CSVFileRecord(nationalId, name, amount, currentFileName + "__" + timeStamp, i, "bad", "wrong data : " + ex.getMessage());
                // stream
                streamService.streamRecord(csvFileRecord);
            }
            csvParser.close();
            reader.close();

        } catch (Exception ex) {
            logger.error(ex.getMessage());
            csvFileRecord = new CSVFileRecord("", "", -1, currentFileName + "__" + timeStamp, 0, "bad", "wrong file format : " + ex.getMessage());
            // stream
            streamService.streamRecord(csvFileRecord);
            logger.error(ex.getMessage());
        } finally {
            // Archive parsed file
            archiveFile(filePath, timeStamp);
            logger.info("Parse CSV File process finished for file name : " + filePath);
        }
    }

    private void archiveFile(String filePath, long timeStamp) {
        try {
            File file = new File(filePath);
            File archiveDir = new File(appConfig.getArchiveDir());
            if (!archiveDir.exists()) {
                archiveDir.mkdir();
            }

            File archivedFile = new File(archiveDir, file.getName() + "__" + timeStamp);
            if (!archivedFile.exists()) {

                Files.move(file.toPath(), archivedFile.toPath());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }
}
