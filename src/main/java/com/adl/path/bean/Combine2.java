package com.adl.path.bean;

import lombok.Data;

import java.util.LinkedList;
import java.util.Map;

@Data
public class Combine2 implements Cloneable {
    private int batchId;
    private int totalCost;
    private LinkedList<Path2> paths = new LinkedList<>();
    private LinkedList<Map<Path2,SharedPath>> sharedSubPaths;

    @Override
    public Combine2 clone() {
        Combine2 combine;
        try {
            combine = (Combine2) super.clone();
            combine.setPaths(new LinkedList<>(this.paths));
            // needn't clone each Path
            return combine;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
