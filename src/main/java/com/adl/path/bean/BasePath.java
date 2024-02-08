package com.adl.path.bean;

import lombok.Data;

@Data
public class BasePath {
    private String pathStr;
    private int pathCost;
    private String[] nodeIds;

    public BasePath(String pathStr, int pathCost, String[] nodeIds) {
        this.pathStr = pathStr;
        this.pathCost = pathCost;
        this.nodeIds = nodeIds;
    }

    public BasePath() {
    }
}
