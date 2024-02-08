package com.adl.path.service;


import com.adl.path.bean.CombineVo;
import com.adl.path.bean.PathVo;

import java.util.List;

public interface PathService {
    List<List<CombineVo>> findShortestCombine(String sourceName, String targetNames, int quantity);

    List<List<PathVo>> findShortestPaths(String sourceName, String targetNames, int quantity);

    List<PathVo> shortestPathAlgorithmBasedOnDB(String sourceName, String targetNames, int quantity, boolean useLog);
}
