package com.adl.path.bean;

import lombok.Data;

@Data
public class PathDto {
    private int batchId;
    private String source;
    private String target;
    private int totalNode;
    private int totalCost;
    private String path;
    private String createdBy;

}
