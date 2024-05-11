package com.evision.csv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${sftp.localPath}")
    private String localPath;

    @Value("${archiveDir}")
    private String archiveDir;

    @Value("${outputDir}")
    private String outputDir;

    @Value("${app.threadNumbers}")
    private String threadNumbers;


    public String getLocalPath()
    {
        return localPath;
    }
    public String getThreadNumbers()
    {
        return threadNumbers;
    }
    public String getArchiveDir()
    {
        return archiveDir;
    }
    public String getOutputDir()
    {
        return outputDir;
    }
}