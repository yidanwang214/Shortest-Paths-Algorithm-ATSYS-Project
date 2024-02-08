package com.adl.path.bean;

public class SubPath extends Node {
    private Node begin;
    private Node end;
    public SubPath(Node begin, Node end) {
        this.begin=begin;
        this.end=end;
        super.setTotalCost(end.getTotalCost());
        super.setChildren(end.getChildren());
        super.setChildCount(end.getChildCount());
    }
}
