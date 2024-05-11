package com.evision.csv.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SFTPLoginDTO {
    private String serverIp;
    private String serverPort;
    private String serverUser;
    private String serverPass;
    private String sshKeyPath;
    private String sshKeyPass;
    private String directoryPath;
}
