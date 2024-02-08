package com.adl.path.controller;

import com.adl.path.bean.PathVo;
import com.adl.path.bean.Resp;
import com.adl.path.service.PathService;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/path")
@RestController
@Validated
public class PathController {

    @Resource
    private PathService pathService;

    @CrossOrigin(origins = "*")
    @GetMapping("/findShortestCombines/{sourceName}/{targetNames}/{quantity}")
    public Resp findShortestCombine(@PathVariable @NotEmpty String sourceName, @PathVariable @NotEmpty String targetNames, @PathVariable @Min(value = 1) int quantity){
        try {
            List combines = pathService.findShortestCombine(sourceName,targetNames, quantity);
            if (combines==null){
                return Resp.fail("no combine among source and targets");
            }
        return Resp.success(combines);
        } catch (Exception e) {
            return Resp.fail(e.getMessage());
        }
    }
    @CrossOrigin(origins = "*")
    @GetMapping("/findShortestPaths/{sourceName}/{targetNames}/{quantity}")
    public Resp findShortestPaths(@PathVariable String sourceName,@PathVariable String targetNames,@PathVariable int quantity){
        return findShortestPaths(sourceName,targetNames,quantity,false);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/findShortestPaths/{sourceName}/{targetNames}/{quantity}/{dbAlgorithm}")
    public Resp findShortestPaths(@PathVariable String sourceName,@PathVariable String targetNames,@PathVariable int quantity,@PathVariable boolean dbAlgorithm){
        return findShortestPaths(sourceName,targetNames,quantity,dbAlgorithm,false);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/findShortestPaths/{sourceName}/{targetNames}/{quantity}/{dbAlgorithm}/{useLog}")
    public Resp findShortestPaths(@PathVariable @NotBlank String sourceName, @PathVariable @NotBlank String targetNames, @PathVariable @Min(1) int quantity, @PathVariable boolean dbAlgorithm, @PathVariable boolean useLog){
        List<List<PathVo>> pathGroups=null;
        try {
            if (dbAlgorithm){
                List<PathVo> paths = pathService.shortestPathAlgorithmBasedOnDB(sourceName,targetNames, quantity, useLog);
                System.out.println(paths.size());
                System.out.println(paths.get(0));
                if (paths!=null&&paths.size()>0){
                    pathGroups = new ArrayList<>(paths.stream().collect(Collectors.groupingBy(n->n.getTarget())).values());
                }
            }else {
                pathGroups = pathService.findShortestPaths(sourceName,targetNames, quantity);
            }
            if (pathGroups==null){
                return Resp.fail("no path among source and targets");
            }
        } catch (Exception e) {
            return Resp.fail(e.getMessage());
        }
        return Resp.success(pathGroups);
    }

}
