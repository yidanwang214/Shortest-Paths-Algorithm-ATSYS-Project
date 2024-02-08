package com.adl.path.bean;

import lombok.Data;

@Data
public class ConnectionExt extends Connection {
    private String sName;
    private String dName;
    private String sType;
    private String dType;
}
