package com.adl.path.bean;

import lombok.Data;

@Data
public class Connection {
    private int id;
    private int sourceDevice;
    private int destinationDevice;
    private int weight;
}
