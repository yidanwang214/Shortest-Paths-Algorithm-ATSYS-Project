package com.adl.path.bean;

import lombok.Data;

@Data
public class Path extends BasePath {
    private int batchId;
    private String[] nodeNames;
    private String[] nodeCosts;
    private String[] edgeCosts;

    public Path(int batchId, String[] nodeIds, String[] nodeNames, int pathCost, String[] nodeCosts, String[] edgeCosts) {
        super(null,pathCost,nodeIds);
        this.batchId = batchId;
        this.nodeNames = nodeNames;
        this.nodeCosts = nodeCosts;
        this.edgeCosts = edgeCosts;
    }
}
