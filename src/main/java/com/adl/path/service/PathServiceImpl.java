package com.adl.path.service;

import com.adl.path.bean.*;
import com.adl.path.dao.PathDao;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static com.adl.path.enums.DeviceType.Destination;
import static com.adl.path.enums.DeviceType.Source;

@Service
public class PathServiceImpl implements PathService {
    private Random random = new Random();
    @Resource
    private DataSource dataSource;
    @Resource
    private DeviceService deviceService;
    @Resource
    private ConnService connService;
    @Resource
    private PathDao pathDao;

    @Override
    public List<List<CombineVo>> findShortestCombine(String sourceName, String targetNames, int quantity){
        // get whole connections and build connect map
        Map<Integer, Device> deviceIdMap = new HashMap<>();
        Map<String, Device> deviceNameMap = new HashMap<>();
        Map<Integer, List<ConnectionExt>> connMap = new HashMap<>();
        Set targetIdSet = new HashSet();
        Device source = buildDeviceAndConnMaps(sourceName,targetNames,deviceIdMap,deviceNameMap,connMap,targetIdSet);
        // build whole tree from source (cut off cycle and the paths not leading to targets)
        Map<Integer,List<Node>> targetPathsMap = new HashMap<>();
        buildTree(targetPathsMap,source,connMap,deviceIdMap,targetIdSet);
        if (targetPathsMap.isEmpty()){
            return null;
        }
        // use sub-path to reduce the tree (Node, SubPath)
        //buildSubPathNode(root);
        // dfs find all paths for each target, get target-paths map
        Map<Integer,List<Path2>> targetPaths = transferTargetNodes2Paths(targetPathsMap);
        // build combines and get the best n combines
        List<Combine2> combines = buildCombinesByPaths(targetPaths, quantity);
        // format path, and subpath for each combine
        combines = calcSharedCost(combines);
        combines=formatPaths(combines);
        // build combineVO for front-end
        List<List<CombineVo>> combineVos = buildCombineVosAndSaveDB(combines);

        return combineVos;
    }

    private static List<Combine2> formatPaths(List<Combine2> combines) {
        for (Combine2 combine : combines) {
            for (Path2 path : combine.getPaths()) {
                if (!path.isFormatted()){
                    // reconstruct shared path, and format path
                    StringBuilder sharedSB = new StringBuilder();
                    StringBuilder tmpSharedSB = new StringBuilder();
                    StringBuilder pathSB = new StringBuilder();
                    StringBuilder formatPathSB = new StringBuilder();
                    boolean onProcess=false;
                    BitSet sharedNodeBit = path.getSharedNodeBit();
                    String[] nodeNames = path.getNodeNames();
                    for (int i = 0; i < nodeNames.length; i++) {
                        pathSB.append("->").append(nodeNames[i]);
                        boolean match = sharedNodeBit.get(i);
                        if (match&&!onProcess) {
                            onProcess=true;
                            formatPathSB.append("-><B><I>").append(nodeNames[i]);
                            if (tmpSharedSB.length()>0){
                                sharedSB.append(',').append(tmpSharedSB.substring(2));
                                tmpSharedSB = new StringBuilder();
                            }
                        }else if (!match&&onProcess){
                            onProcess=false;
                            formatPathSB.append("</I></B>->").append(nodeNames[i]);
                        }else {
                            formatPathSB.append("->").append(nodeNames[i]);
                        }
                        // shared path
                        if (match){
                            tmpSharedSB.append("->").append(nodeNames[i]);
                        }
                    }

                    // check if on process at the end point
                    if (onProcess){
                        formatPathSB.append("</B>");
                        if (tmpSharedSB.length()>0){
                            sharedSB.append(',').append(tmpSharedSB.substring(2));
                        }
                    }
                    // set str
                    path.setPathStr(pathSB.substring(2));
                    path.setFormatPathStr(formatPathSB.substring(2));
                    if (sharedSB.length()>0){
                        path.setSharedStr(sharedSB.substring(1));
                    }else {
                        path.setSharedStr("");
                    }

                }
            }
        }
        return combines;
    }

    private List<List<PathVo>> buildPathVosAndSaveDB(List<List<Path2>> pathGroups) {
        int batchId = 100000 + random.nextInt(900000);
        List<PathDto> saveData = new ArrayList<>();
        List<List<PathVo>> pathList = new ArrayList<>();
        for (List<Path2> pathGroup : pathGroups) {
            List<PathVo> vos = new ArrayList<>();
            for (Path2 path : pathGroup) {
                PathVo pathVo = new PathVo();
                pathVo.setBatchId(batchId);
                String[] nodeNames = path.getNodeNames();
                pathVo.setSource(path.getNodeNames()[0]);
                pathVo.setTarget(path.getNodeNames()[nodeNames.length-1]);
                StringBuilder sb = new StringBuilder();
                for (String nodeName : nodeNames) {
                    sb.append("->").append(nodeName);
                }
                pathVo.setPath(sb.substring(2));
                pathVo.setTotalNode(nodeNames.length);
                pathVo.setTotalCost(path.getPathCost());
                pathVo.setCreatedBy("findShortestPaths");
                saveData.add(pathVo);
                vos.add(pathVo);
            }
            pathList.add(vos);
        }
        pathDao.savePaths(saveData);
        return pathList;
    }
    private List<List<CombineVo>> buildCombineVosAndSaveDB(List<Combine2> combines) {
        // calc shared cost
        calcSharedCost(combines);
        // build data
        List<CombineDto> saveData = new ArrayList<>();
        List<List<CombineVo>> combineList = new ArrayList<>();
        int batchId = 100000 + random.nextInt(900000);
        int combineNumber = 1;
        for (Combine2 combine : combines) {
            List<CombineVo> vos = new ArrayList<>();
            for (Path2 path : combine.getPaths()) {
                CombineVo combineVo = new CombineVo();
                combineVo.setBatchId(batchId);
                combineVo.setCombineNumber(combineNumber);
                combineVo.setCombineCost(combine.getTotalCost());
                combineVo.setPathCost(path.getPathCost());
                combineVo.setSharedPathCost(path.getSharedCost());
                combineVo.setPath(path.getPathStr());
                combineVo.setFormattedPath(path.getFormatPathStr());
                combineVo.setSharedPath(path.getSharedStr());
                combineVo.setCreatedBy("findShortestCombines");
                saveData.add(combineVo);
                vos.add(combineVo);
            }
            combineList.add(vos);
            combineNumber++;
        }
        pathDao.saveCombines(saveData);
        return combineList;
    }

    private List<Combine2> calcSharedCost(List<Combine2> combines) {
        for (Combine2 combine : combines) {
            LinkedList<Path2> paths = combine.getPaths();
            // initialize
            for (int i = 0; i < paths.size(); i++) {
                paths.set(i,paths.get(i).clone());
            }
            // compare each pair paths
            for (int i = 0; i < paths.size(); i++) {
                // each path
                Path2 path = paths.get(i);
                for (int j = i + 1; j < paths.size(); j++) {
                    comparePathsPair(path, paths.get(j));
                }
            }
            // calc shared cost
            for (Path2 path : paths) {
                int sharedCost=0;
                int[] nodeCosts = path.getNodeCosts();
                int[] edgeCosts = path.getEdgeCosts();
                BitSet sharedNodeBit = path.getSharedNodeBit();
                for (int j = sharedNodeBit.nextSetBit(0); j >= 0; j = sharedNodeBit.nextSetBit(j + 1)) {
                    sharedCost += nodeCosts[j];
                }
                BitSet sharedEdgeBit = path.getSharedEdgeBit();
                for (int j = sharedEdgeBit.nextSetBit(0); j >= 0; j = sharedEdgeBit.nextSetBit(j + 1)) {
                    sharedCost += edgeCosts[j];
                }
                path.setSharedCost(sharedCost);
            }
        }
        return combines;
    }

    private Map<Integer, List<Path2>> transferTargetNodes2Paths(Map<Integer, List<Node>> targetNodesMap) {
        int id=0;
        Map<Integer, List<Path2>> pathMap = new HashMap();
        for (Map.Entry<Integer, List<Node>> entry : targetNodesMap.entrySet()) {
            List<Path2> paths = new ArrayList<>();
            for (Node node : entry.getValue()) {
                int depth = node.getDepth();
                Path2 path = new Path2();
                path.setId(++id);
                path.setPathCost(node.getTotalCost());
                String[] nodeNames = new String[depth + 1];
                int[] nodeIds = new int[depth + 1];
                int[] nodeCosts = new int[depth + 1];
                int[] edgeIds = new int[depth + 1];
                int[] edgeCosts = new int[depth + 1];
                BitSet sharedNodeBit = new BitSet(depth + 1);
                BitSet sharedEdgeBit = new BitSet(depth + 1);
                do{
                    depth = node.getDepth();
                    nodeNames[depth]=node.getDevice().getName();
                    nodeIds[depth]=node.getDevice().getId();
                    edgeIds[depth]=node.getConnId();
                    nodeCosts[depth]=node.getDevice().getCost();
                    edgeCosts[depth]=node.getConnCost();
                    node=node.getParent();
                }
                while (node!=null);
                path.setNodeNames(nodeNames);
                path.setNodeIds(nodeIds);
                path.setEdgeIds(edgeIds);
                path.setNodeCosts(nodeCosts);
                path.setEdgeCosts(edgeCosts);
                path.setSharedNodeBit(sharedNodeBit);
                path.setSharedEdgeBit(sharedEdgeBit);
                paths.add(path);
            }
            pathMap.put(entry.getKey(),paths);
        }
        return pathMap;
    }

    public static List<Combine2> buildCombinesByPaths(Map<Integer, List<Path2>> targetPaths, int maxCombine) {
        targetPaths.forEach((k,v)->{
            System.out.println("target:"+k);
            for (Path2 path2 : v) {
                System.out.print(path2.getId()+":");
                for (String nodeName : path2.getNodeNames()) {
                    System.out.print(nodeName+"->");
                }
                System.out.println();
            }
        });

        // get combines
        List<Integer> keys = new ArrayList<>(targetPaths.keySet());
        List<Combine2> combines = getCombinesByPathsRecursive(targetPaths, keys, 0,new Combine2());
        // calculate cost for combine
        combines.forEach(n-> calcCombineCost(n));
        // sort and filter combines
        PriorityQueue<Combine2> queue = new PriorityQueue<>((c1,c2)->{
            int t1 = c1.getTotalCost();
            int t2 = c2.getTotalCost();
            return t1==t2?0:(t1>t2)?1:-1;
        });
        combines.forEach(n->queue.offer(n));
        List<Combine2> list = new ArrayList<>(maxCombine);
        for (int i = 0; i < maxCombine; i++) {
            Combine2 poll = queue.poll();
            if (poll!=null){
                list.add(poll);
            }
        }
        return list;
    }

    private static void calcCombineCost(Combine2 combine) {
        // calculate cost
        Set<Integer> nodeIdSet = new HashSet<>();
        Set<Integer> edgeIdSet = new HashSet<>();
        int totalCost=0;
        for (Path2 path : combine.getPaths()) {
            int[] nodeIds = path.getNodeIds();
            int[] edgeIds = path.getEdgeIds();
            int[] nodeCosts = path.getNodeCosts();
            int[] edgeCosts = path.getEdgeCosts();
            for (int i = 0; i < nodeIds.length; i++) {
                if (!edgeIdSet.contains(edgeIds[i])){
                    totalCost+=edgeCosts[i];
                    edgeIdSet.add(edgeIds[i]);
                }
                if (!nodeIdSet.contains(nodeIds[i])){
                    totalCost+=nodeCosts[i];
                    nodeIdSet.add(nodeIds[i]);
                }
            }
        }
        combine.setTotalCost(totalCost);
    }
    /**
     *  find shared sub-paths for each pair
     * @param path1
     * @param path2
     */
    private static void comparePathsPair(Path2 path1, Path2 path2) {
        int[] nodeIds1=path1.getNodeIds();
        int[] nodeIds2=path2.getNodeIds();
        BitSet sharedBitN1 = path1.getSharedNodeBit();
        BitSet sharedBitN2 = path2.getSharedNodeBit();
        BitSet sharedBitE1 = path1.getSharedEdgeBit();
        BitSet sharedBitE2 = path2.getSharedEdgeBit();
        int compInd=0;
        boolean onProcess=false;
        f:for (int i = 0; i < nodeIds1.length; i++) {
            for (int j = compInd; j < nodeIds2.length; j++) {
                if (nodeIds1[i] == nodeIds2[j]) {
                    // shared nodes
                    sharedBitN1.set(i);
                    sharedBitN2.set(j);
                    // shared edges
                    if (onProcess){
                        sharedBitE1.set(i);
                        sharedBitE2.set(j);
                    }else {
                        onProcess=true;
                    }
                    compInd = j+1;
                    continue f;

                }else {
                    onProcess=false;
                }

            }
        }
    }

    private static List<Combine2> getCombinesByPathsRecursive(Map<Integer, List<Path2>> targetPaths, List<Integer> keys, int keyIndex, Combine2 curCombine) {
        Integer targetId = keys.get(keyIndex);
        List<Combine2> all = new ArrayList<>();
        for (Path2 path : targetPaths.get(targetId)) {
            Combine2 combine = curCombine.clone();
            LinkedList<Path2> paths = combine.getPaths();
            paths.add(path);
            if (keyIndex==keys.size()-1){
                all.add(combine);
            }else {
                List<Combine2> rst = getCombinesByPathsRecursive(targetPaths, keys, keyIndex + 1, combine);
                all.addAll(rst);
            }
        }
        return all;
    }

    private Node buildTree(Map<Integer,List<Node>> targetPathsMap, Device device, Map<Integer, List<ConnectionExt>> connMap, Map<Integer, Device> deviceIdMap, Set targetSet) {
        Node root = new Node();
        root.setDeviceCost(device.getCost());
        root.setTotalCost(device.getCost());
        root.setDevice(device);
        root.setDepth(0);
        buildTree(targetPathsMap,root,connMap,deviceIdMap,targetSet,new HashSet<>(),new HashSet<>());
        return root;
    }

    private int buildTree(Map<Integer,List<Node>> targetPathsMap, Node current, Map<Integer, List<ConnectionExt>> connMap, Map<Integer, Device> deviceIdMap, Set<Integer> targetSet, Set<Integer> visited, Set<Integer> skipSet) {
        // achieve target
        if (Destination.name().equalsIgnoreCase(current.getDevice().getType())){
            // save target nodes
            int id = current.getDevice().getId();
            if (targetSet.contains(id)){
                List<Node> targetPath = targetPathsMap.get(id);
                if (targetPath==null){
                    targetPath=new ArrayList<>();
                }
                targetPath.add(current);
                targetPathsMap.put(id,targetPath);
                return 0;
            }else {
                // skip other targets
                return -1;
            }
        }
        int count=0;
        List<ConnectionExt> conns = connMap.get(current.getDevice().getId());
        if (conns!=null) {
            for (ConnectionExt conn : conns) {
                int dest = conn.getDestinationDevice();
                // skip
                if (skipSet.contains(dest)){
                    continue;
                }
                if (!visited.contains(dest)){
                    visited.add(dest);
                    Node child = new Node();
                    child.setParent(current);
                    Device des = deviceIdMap.get(dest);
                    child.setDevice(des);
                    child.setDeviceCost(des.getCost());
                    child.setConnId(conn.getId());
                    child.setConnCost(conn.getWeight());
                    child.setTotalCost(current.getTotalCost()+conn.getWeight()+des.getCost());
                    child.setDepth(current.getDepth()+1);
                    int rst = buildTree(targetPathsMap,child, connMap, deviceIdMap, targetSet, visited,skipSet);
                    if (rst>=0){
                        List<Node> children = current.getChildren();
                        if (children==null){
                            children=new ArrayList<>();
                        }
                        children.add(child);
                        count++;
                        // related devices can be reused, devices without relationship can be skipped next time
                        visited.remove(dest);
                    }else {
                        skipSet.add(dest);
                    }
                }
            }
        }
        // a non-destination-node who has no child can be cut off
        if (count==0) {
            count=-1;
        }else {
            current.setChildCount(count);
        }

        return count;
    }

    private Device buildDeviceAndConnMaps(String sourceName, String targetNames, Map<Integer, Device> deviceIdMap, Map<String, Device> deviceNameMap, Map<Integer, List<ConnectionExt>> connMap, Set<Integer> targetIdSet) {
        // build device map
        List<Device> devices = deviceService.listAvailableDevices();
        for (Device d : devices) {
            deviceIdMap.put(d.getId(),d);
            deviceNameMap.put(d.getName(),d);
        }
        // check source
        Device source = deviceNameMap.get(sourceName.trim());
        if (source==null){
            throw new RuntimeException(String.format("source %s doesn't exist or is unavailable",sourceName));
        }
        if (!Source.name().equalsIgnoreCase(source.getType())){
            throw new RuntimeException(String.format("source %s is not a source Device",sourceName));
        }
        // check targets
        for (String s : targetNames.split(",")) {
            Device d = deviceNameMap.get(s.trim());
            if (d==null){
                throw new RuntimeException(String.format("destination %s doesn't exist or is unavailable",s));
            }
            if (!Destination.name().equalsIgnoreCase(d.getType())){
                throw new RuntimeException(String.format("destination %s is not a destination Device",s));
            }
            targetIdSet.add(d.getId());
        }
        // build conn map
        List<ConnectionExt> conns = connService.listAvailableConn();
        for (ConnectionExt conn : conns) {
            List<ConnectionExt> list = connMap.get(conn.getSourceDevice());
            if (list==null){
                list=new ArrayList<>();
                connMap.put(conn.getSourceDevice(),list);
            }
            list.add(conn);
        }
        return source;
    }


    @Override
    public List<List<PathVo>> findShortestPaths(String sourceName, String targetNames, int quantity) {
        // get whole connections and build connect map
        Map<Integer, Device> deviceIdMap = new HashMap<>();
        Map<String, Device> deviceNameMap = new HashMap<>();
        Map<Integer, List<ConnectionExt>> connMap = new HashMap<>();
        Set targetIdSet = new HashSet();
        Device source = buildDeviceAndConnMaps(sourceName,targetNames,deviceIdMap,deviceNameMap,connMap,targetIdSet);
        // build whole tree from source (cut off cycle and the paths not leading to targets)
        Map<Integer,List<Node>> targetPathsMap = new HashMap<>();
        buildTree(targetPathsMap,source,connMap,deviceIdMap,targetIdSet);
        if (targetPathsMap.isEmpty()){
            return null;
        }
        // dfs find all paths for each target, get target-paths map
        Map<Integer,List<Path2>> targetPaths = transferTargetNodes2Paths(targetPathsMap);
        // choose the cheapest n path for each target
        List<List<Path2>> pathGroups = chooseCheapestNPath(targetPaths, quantity);
        // build pathvo for front-end, save path
        List<List<PathVo>> pathVos = buildPathVosAndSaveDB(pathGroups);

        return pathVos;
    }

    private List<List<Path2>> chooseCheapestNPath(Map<Integer, List<Path2>> targetPathsMap, int maxPath) {

        List<List<Path2>> pathGroups = new ArrayList<>();
        targetPathsMap.forEach((k,v)->{
            // sort and filter combines
            PriorityQueue<Path2> queue = new PriorityQueue<>((c1,c2)->{
                int t1 = c1.getPathCost();
                int t2 = c2.getPathCost();
                return t1==t2?0:(t1>t2)?1:-1;
            });
            for (Path2 path2 : v) {
                queue.offer(path2);
            }
            List<Path2> pathGroup = new ArrayList<>();
            for (int i = 0; i < maxPath; i++) {
                Path2 poll = queue.poll();
                if (poll!=null){
                    pathGroup.add(poll);
                }
            }
            pathGroups.add(pathGroup);
        });
        return pathGroups;
    }

    private void fillPathStr4Combine(List<Combine> combines) {
        for (Combine combine : combines) {
            for (Path path : combine.getPaths()) {
                path.setPathStr(String.join("->",path.getNodeNames()));
            }
        }
    }

    private List<CombineDto> buildCombineDtos(List<Combine> combines) {
        List<CombineDto> saveData = new ArrayList<>();
        int combineNumber = 1;
        for (Combine combine : combines) {
            LinkedList<Path> paths = combine.getPaths();
            LinkedList<Map<Path, SharedPath>> sharedSubPaths = combine.getSharedSubPaths();
            for (int i = 0; i < paths.size(); i++) {
                CombineVo combineVo = new CombineVo();
                combineVo.setCombineNumber(combineNumber);
                combineVo.setBatchId(combine.getBatchId());
                combineVo.setCombineCost(combine.getTotalCost());
                combineVo.setPathCost(paths.get(i).getPathCost());
                Set<String> sharedPathSet = new HashSet();
                int sharedCost = 0;
                for (SharedPath sharedPath : sharedSubPaths.get(i).values()) {
                    if (!sharedPathSet.contains(sharedPath.getPathStr())){
                        sharedPathSet.add(sharedPath.getPathStr());
                        sharedCost += sharedPath.getPathCost();
                    }
                }
                // construct format path and rebuild sharedPath
                combineVo.setSharedPathCost(sharedCost);
                combineVo.setCreatedBy("findShortestCombine");
                saveData.add(combineVo);
            }
            combineNumber++;
        }
        return saveData;
    }



    /**
     * order combines by total cost and keep the first N combines
     * @param combines
     * @param maxCombine
     * @return
     */
    private List<Combine> getFirstNCombine(List<Combine> combines, int maxCombine) {
        PriorityQueue<Combine> queue = new PriorityQueue<>((c1,c2)->{
            int t1 = c1.getTotalCost();
            int t2 = c2.getTotalCost();
            return t1==t2?0:(t1>t2?1:-1);
        });
        for (Combine combine : combines) {
            queue.offer(combine);
        }
        List<Combine> rst = new ArrayList<>();
        int count=0;
        while (!queue.isEmpty()&&count<maxCombine){
            Combine poll = queue.poll();
            if (poll!=null){
                rst.add(poll);
            }
            count++;
        }
        return rst;
    }


    /**
     * build combine from paths data
     * @param targetListMap
     * @return
     */
    private List<Combine> buildCombines(Map<Integer, List<Path>> targetListMap) {
        // build combination
        List<Path>[] array = (List<Path>[]) targetListMap.values().toArray(new List[0]);
        List<Combine> combineList = new ArrayList<>();
        buildCombines(array,0,new Combine(),combineList);
        // deal sub path and cost for each combine
        for (Combine combine : combineList) {
            PathHelper.findSharedSubPaths(combine);
            PathHelper.calcCost4Combine(combine);
        }
        return combineList;
    }


    /**
     * recursively build combine from paths data
     * @param pathListArr
     * @param index
     * @param curCombine
     * @param combineList
     */
    private void buildCombines(List<Path>[] pathListArr, int index, Combine curCombine, List<Combine> combineList) {
        List<Path> list = pathListArr[index];
        for (int i = 0; i < list.size(); i++) {
            Combine newCombine = curCombine.clone();
            newCombine.setBatchId(list.get(i).getBatchId());
            //newCombine.setTotalCost(newCombine.getTotalCost()+list.get(i).getPathCost());
            newCombine.getPaths().addLast(list.get(i));
            // the last level
            if (index==pathListArr.length-1){
                combineList.add(newCombine);
            }else {
                buildCombines(pathListArr,index+1, newCombine,combineList);
            }
        }
    }

    private Map<Integer, List<Path>> getTargetListMap(String sourceName, String targetNames, int maxCombine, boolean useLog){
        Map<Integer, List<Path>> targetListMap = new HashMap<>();
        try (java.sql.Connection conn = dataSource.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call findShortestPaths(?, ?, ?, ?, ?)}");
            cs.setString(1, sourceName);
            cs.setString(2, targetNames);
            cs.setInt(3, maxCombine);
            cs.setBoolean(4, useLog);
            cs.setBoolean(5, true);
            boolean hasResults = cs.execute();
            for (int i = 0; i < 3; i++) {
                hasResults=cs.getMoreResults();
            }
            if (hasResults){
                // get batchId
                ResultSet rs = cs.getResultSet();
                rs.next();
                int batchId = rs.getInt("batchId");
                // get paths
                cs.getMoreResults();
                rs = cs.getResultSet();
                while (rs.next()) {
                    int targetId = rs.getInt("current_node_id");
                    int cost = rs.getInt("comulative_cost");
                    String nodeIds = rs.getString("node_ids").substring(1);
                    String nodeNames = rs.getString("node_names");
                    String nodeCosts = rs.getString("node_costs");
                    String edgeCosts = rs.getString("edge_costs");
                    List<Path> list = targetListMap.get(targetId);
                    if (list==null){
                        list=new ArrayList<>();
                        targetListMap.put(targetId,list);
                    }
                    list.add(new Path(batchId,nodeIds.split(","),nodeNames.split("->"),cost,nodeCosts.split(","),edgeCosts.split(",")));
                }
            }
            cs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return targetListMap;
    }

    @Override
    public List<PathVo> shortestPathAlgorithmBasedOnDB(String sourceName, String targetNames, int quantity, boolean useLog){
        return pathDao.findShortestPaths(sourceName, targetNames, quantity, useLog);
    }
    public List<PathVo> shortestPathAlgorithmBasedOnDB2(String sourceName, String targetNames, int quantity, boolean useLog){
        List<PathVo> paths = new ArrayList<>();
        try (java.sql.Connection conn = dataSource.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call findShortestPaths(?, ?, ?, ?, ?)}");
            cs.setString(1, sourceName);
            cs.setString(2, targetNames);
            cs.setInt(3, quantity);
            cs.setBoolean(4, useLog);
            cs.setBoolean(5, true);
            if (cs.execute()){
                // get paths
                ResultSet rs = cs.getResultSet();
                while (rs.next()) {
                    PathVo pathVo = new PathVo();
                    pathVo.setBatchId(rs.getInt("batchId"));
                    pathVo.setSource(rs.getString("source"));
                    pathVo.setTarget(rs.getString("Destination"));
                    pathVo.setPath(rs.getString("Path"));
                    pathVo.setTotalNode(rs.getInt("NodeCount"));
                    pathVo.setTotalCost(rs.getInt("TotalCost"));
                    paths.add(pathVo);
                }
            }
            cs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return paths;
    }

}
