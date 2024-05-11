package com.evision.csv.collector.sftp;




import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Vector;

public class SFTPFunctions {

    Logger logger = LogManager.getLogger(SFTPFunctions.class);

    Session session;
    String doneFormat = "EV.RECO.DONE";

    public ChannelSftp connectToSFTP(String serverIp,
                                     int port,
                                     String username,
                                     String password,
                                     String sshKeyPath,
                                     String sshKeyPassphrase) {
        try {
            logger.debug("connectToSFTP process started");

            JSch jsch = new JSch();
            if (sshKeyPath != null && sshKeyPassphrase != null && sshKeyPath.length() > 1 && sshKeyPassphrase.length() > 1) {
                jsch.addIdentity(sshKeyPath, sshKeyPassphrase);
            }

            session = jsch.getSession(username, serverIp, port);
            session.setPassword(password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;

            logger.debug("SFTP Connection has been Opened Successfully");

            return channelSftp;
        } catch (Exception e) {
            logger.error("Error in connectToSFTP : " + e.getMessage());
            return null;
        }

    }

    public void downloadFile(ChannelSftp channelSftp,
                             String SFTPDirectory,
                             String SFTPFileName,
                             String localFilePath,
                             String localFileName,
                             String fileNameCondition,
                             String fileNameConditionValue,
                             String originalFileAction
    ) {
        try {
            logger.debug("downloadFile process started");

            // change SFTP Directory
            channelSftp.cd(SFTPDirectory);
            logger.debug("change SFTP Directory to : " + SFTPDirectory);
            // check SFTP file name
            if (SFTPFileName != null && SFTPFileName.equals("*")) {
                Vector<ChannelSftp.LsEntry> filesList = channelSftp.ls("*");
                if (fileNameCondition == null || fileNameCondition.isEmpty() || fileNameCondition.isBlank()) {
                    logger.debug("download all files in the SFTP path");
                    for (int i = 0; i < filesList.size(); i++) {
                        ChannelSftp.LsEntry entry = filesList.get(i);
                        if (entry.getAttrs().isDir() == false && !entry.getFilename().toUpperCase().contains(doneFormat)) {
                            executeDownloadActions(channelSftp, entry, localFilePath, originalFileAction);
                        }
                    }
                } else if (fileNameCondition.toUpperCase().equals("START_WITH")) {
                    logger.debug("download all files in the SFTP path that START_WITH : " + fileNameConditionValue.toUpperCase());
                    for (int i = 0; i < filesList.size(); i++) {
                        ChannelSftp.LsEntry entry = filesList.get(i);
                        if (entry.getAttrs().isDir() == false
                                && !entry.getFilename().toUpperCase().contains(doneFormat)
                                && entry.getFilename().toUpperCase().startsWith(fileNameConditionValue.toUpperCase())
                        ) {
                            executeDownloadActions(channelSftp, entry, localFilePath, originalFileAction);
                        }
                    }
                } else if (fileNameCondition.toUpperCase().equals("END_WITH")) {
                    logger.debug("download all files in the SFTP path that END_WITH : " + fileNameConditionValue);
                    for (int i = 0; i < filesList.size(); i++) {
                        ChannelSftp.LsEntry entry = filesList.get(i);
                        if (entry.getAttrs().isDir() == false
                                && !entry.getFilename().toUpperCase().contains(doneFormat)
                                && entry.getFilename().toUpperCase().endsWith(fileNameConditionValue.toUpperCase())
                        ) {
                            executeDownloadActions(channelSftp, entry, localFilePath, originalFileAction);
                        }
                    }
                } else if (fileNameCondition.toUpperCase().equals("CONTAINS")) {
                    logger.debug("download all files in the SFTP path that CONTAINS : " + fileNameConditionValue);
                    for (int i = 0; i < filesList.size(); i++) {
                        ChannelSftp.LsEntry entry = filesList.get(i);
                        if (entry.getAttrs().isDir() == false
                                && !entry.getFilename().toUpperCase().contains(doneFormat)
                                && entry.getFilename().toUpperCase().contains(fileNameConditionValue.toUpperCase())
                        ) {
                            executeDownloadActions(channelSftp, entry, localFilePath, originalFileAction);
                        }
                    }
                } else if (fileNameCondition.toUpperCase().equals("EQUALS")) {
                    logger.debug("download all files in the SFTP path that EQUALS : " + fileNameConditionValue);
                    for (int i = 0; i < filesList.size(); i++) {
                        ChannelSftp.LsEntry entry = filesList.get(i);
                        if (entry.getAttrs().isDir() == false
                                && !entry.getFilename().toUpperCase().contains(doneFormat)
                                && entry.getFilename().toUpperCase().equals(fileNameConditionValue.toUpperCase())
                        ) {
                            executeDownloadActions(channelSftp, entry, localFilePath, originalFileAction);
                        }
                    }
                }
                else if (fileNameCondition.toUpperCase().equals("REGEX")) {
                    logger.debug("download all files in the SFTP path that MATCH REGEX : " + fileNameConditionValue);
                    for (int i = 0; i < filesList.size(); i++) {
                        ChannelSftp.LsEntry entry = filesList.get(i);
                        if (entry.getAttrs().isDir() == false
                                && !entry.getFilename().toUpperCase().contains(doneFormat)
                                && entry.getFilename().matches(fileNameConditionValue)
                        ) {
                            executeDownloadActions(channelSftp, entry, localFilePath, originalFileAction);
                        }
                    }
                }

            } else if (SFTPFileName != null && !SFTPFileName.isBlank() && !SFTPFileName.isEmpty()) {
                logger.debug("download single file only file name is : " + SFTPFileName);
                if (checkIfFileExist(channelSftp, SFTPFileName)) {
                    // try to rename the file to ensure that file not in another process
                    if (!rename(SFTPFileName, SFTPFileName, channelSftp)) {
                        logger.warn(" File name is : " + SFTPFileName + " in another process");
                    } else {
                        // download file
                        channelSftp.get(SFTPFileName, localFilePath + "/" + localFileName);
                        logger.debug(" File name is : " + localFileName + " has been downloaded successfully");

                        if (originalFileAction.toUpperCase().equals("DELETE")) {
                            channelSftp.rm(SFTPFileName);
                            logger.debug("original SFTP File name : " + localFileName + " has been delete");
                        } else if (originalFileAction.toUpperCase().equals("BACKUP")) {
                            // create backup folder if not exist
                            createBackupFolderIfNotExist(channelSftp);
                            // rename the downloaded file to filenameEV.RECO.DONE + date.getTime and move it to backup folder
                            channelSftp.rename(SFTPFileName, "backup/" + SFTPFileName + doneFormat + "_" + new Date().getTime());
                            logger.debug(" File name is : " + SFTPFileName + " has been renamed and moved to backup/" + SFTPFileName + doneFormat + "_" + new Date().getTime());
                        }
                    }
                }
            }


            // close connection
            channelSftp.exit();
            session.disconnect();
            logger.debug("SFTP connection closed");


            //logger.debug(SFTPDirectory + "/" + SFTPFileName + " file has been downloaded successfully to " + LocalFilePath + "/" + LocalFileName);

            logger.debug("downloadFile process finished");
        } catch (Exception e) {
            logger.error("Error in downloadFile : " + e.getMessage());
        }
    }

    private void executeDownloadActions(ChannelSftp channelSftp, ChannelSftp.LsEntry entry, String localFilePath, String originalFileAction) throws Exception {
        logger.debug(" File name is : " + entry.getFilename());
        // try to rename the file to ensure that file not in another process
        if (!rename(entry.getFilename(), entry.getFilename(), channelSftp)) {
            logger.warn(" File name is : " + entry.getFilename() + " in another process");
        } else {
            // download file
            channelSftp.get(entry.getFilename(), localFilePath + "/" + entry.getFilename());
            logger.debug(" File name is : " + entry.getFilename() + " has been downloaded successfully");
            if (originalFileAction.toUpperCase().equals("DELETE")) {
                channelSftp.rm(entry.getFilename());
                logger.debug("original SFTP File name : " + entry.getFilename() + " has been delete");
            } else if (originalFileAction.toUpperCase().equals("BACKUP")) {
                // create backup folder if not exist
                createBackupFolderIfNotExist(channelSftp);
                // rename the downloaded file to filenameEV.RECO.DONE + date.getTime and move it to backup folder
                channelSftp.rename(entry.getFilename(), "backup/"+entry.getFilename() + doneFormat + "_" + new Date().getTime());
                logger.debug(" File name is : " + entry.getFilename() + " has been renamed to and moved to backup/" + entry.getFilename() + doneFormat + "_" + new Date().getTime());
            }
        }
    }

    private Boolean checkIfFileExist(ChannelSftp channelSftp, String SFTPFileName) {
        try {
            SftpATTRS sftpATTRS = channelSftp.lstat(SFTPFileName);
            logger.debug("file size: " + sftpATTRS.getSize());
            return true;
        } catch (Exception e) {
            logger.debug("file name : " + SFTPFileName + " not exist");
            return false;
        }
    }

    private Boolean rename(String oldName, String newName, ChannelSftp channelSftp) {
        try {
            channelSftp.rename(oldName, newName);
            return true;
        } catch (Exception e) {
            if(e.getMessage().equals("File already exists"))
                return true;
            else
            return false;
        }
    }

    private void createBackupFolderIfNotExist(ChannelSftp channelSftp) {
        try {
            channelSftp.mkdir("backup");
            logger.debug("backup folder has been created");
        } catch (Exception e) {

        }
    }

}
