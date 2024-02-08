package com.adl.path.bean;

import lombok.Data;

import java.util.List;

@Data
public class Node {
    private int deviceCost;
    private int connCost;
    private int totalCost;
    private Device device;
    private Node parent;
    private List<Node> children;
    private int childCount;
    private int depth;
    private int connId;


    public int getConnId() {
        return connId;
    }
}
