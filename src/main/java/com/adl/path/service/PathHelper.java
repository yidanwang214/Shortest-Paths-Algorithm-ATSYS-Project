package com.adl.path.service;

import com.adl.path.bean.Combine;
import com.adl.path.bean.Path;
import com.adl.path.bean.SharedPath;

import java.util.*;

public class PathHelper {

    /**
     * calculate the total cost of combine
     * @param combine
     */
    public static void calcCost4Combine(Combine combine) {
        int totalCost=0;
        Set nodeSet = new HashSet();
        Set edgeSet = new HashSet();
        LinkedList<Path> paths = combine.getPaths();
        for (Path path : paths) {
            String[] nodeIds = path.getNodeIds();
            String[] nodeCosts = path.getNodeCosts();
            String[] edgeCosts = path.getEdgeCosts();
            for (int i = 0; i < nodeCosts.length; i++) {
                String id = nodeIds[i];
                if (!nodeSet.contains(id)) {
                    totalCost+=Integer.valueOf(nodeCosts[i]);
                    nodeSet.add(id);
                }
            }
            for (int i = 1; i < edgeCosts.length; i++) {
                String edge = nodeIds[i-1]+":"+nodeIds[i];
                if (!edgeSet.contains(edge)) {
                    totalCost+=Integer.valueOf(edgeCosts[i]);
                    edgeSet.add(edge);
                }
            }
        }
        combine.setTotalCost(totalCost);
    }

    /**
     * find the shared paths for each combine
     * @param combine
     */
    public static void findSharedSubPaths(Combine combine) {
        LinkedList<Path> paths = combine.getPaths();
        // init sharedSubPaths, each path has a map to store the shared part with other paths
        LinkedList<Map<Path,SharedPath>> sharedSubPaths =  new LinkedList<>();
        for (int i = 0; i < paths.size(); i++) {
            sharedSubPaths.add(new HashMap<>());
        }
        combine.setSharedSubPaths(sharedSubPaths);
        for (int i = 0; i < paths.size(); i++) {
            Path path1 = paths.get(i);
            for (int j = i+1; j < paths.size(); j++) {
                Path path2 = paths.get(j);
                // find shared sub-paths for each pair
                StringBuilder sharedPath = new StringBuilder();
                int sharedCost = findSharedPath4Pair(path1,path2,sharedPath);
                if (sharedPath.length()>0){
                    SharedPath sp = new SharedPath(sharedPath.substring(1),sharedCost);
                    sharedSubPaths.get(i).put(path2,sp);
                    sharedSubPaths.get(j).put(path1,sp);
                }
            }
        }
    }


    /**
     * find shared sub-paths for each pair
     * @param path1
     * @param path2
     * @param sharedPaths
     * @return
     */
    private static int findSharedPath4Pair(Path path1, Path path2, StringBuilder sharedPaths) {
        // each pair
        String[] nodeIds1 = path1.getNodeIds();
        String[] nodeNames = path1.getNodeNames();
        String[] edgeCosts = path1.getEdgeCosts();
        String[] nodeCosts = path1.getNodeCosts();
        String[] nodeIds2 = path2.getNodeIds();
        int totalSharedCost=0;
        int compInd=0;
        for (int i = 0; i < nodeIds1.length; i++) {
            int sharedNodesCount=0;
            int sharedCost=0;
            StringBuilder tmpSharedPath = new StringBuilder();
            for (int j=compInd; j < nodeIds2.length; j++) {
                if (!nodeIds1[i].equals(nodeIds2[j])){
                    // finish a sub-path match
                    if (sharedNodesCount>0) break;
                }else {
                    // start the matching processor
                    sharedNodesCount++;
                    sharedCost+=Integer.valueOf(nodeCosts[i]);
                    if (sharedNodesCount>1){
                        sharedCost+=Integer.valueOf(edgeCosts[i]);
                    }
                    tmpSharedPath.append("->").append(nodeNames[i]);
                    i++;
                    compInd=j+1;
                }
            }
            // sharedCost can be used to control whether considering single shared node or not
            if (sharedNodesCount>0){
                sharedPaths.append(',').append(tmpSharedPath.substring(2));
                totalSharedCost+=sharedCost;
            }
        }
        return totalSharedCost;
    }
}
