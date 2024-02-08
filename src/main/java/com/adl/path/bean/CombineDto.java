package com.adl.path.bean;

import lombok.Data;

import java.util.LinkedList;
import java.util.Map;

@Data
public class CombineDto {
    private int batchId;
    private int combineNumber;
    private int combineCost;
    private String path;
    private int pathCost;
    private String sharedPath;
    private int sharedPathCost;
    private String createdBy;

}
