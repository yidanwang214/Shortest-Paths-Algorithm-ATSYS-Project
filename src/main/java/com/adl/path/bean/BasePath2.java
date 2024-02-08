package com.adl.path.bean;

import lombok.Data;

@Data
public class BasePath2 {
    private String pathStr;
    private String formatPathStr;
    private String sharedStr;
    private int pathCost;
    private int[] nodeIds;
    private int[] edgeIds;

    public BasePath2(String pathStr, int pathCost, int[] nodeIds) {
        this.pathStr = pathStr;
        this.pathCost = pathCost;
        this.nodeIds = nodeIds;
    }

    public BasePath2() {
    }
}
