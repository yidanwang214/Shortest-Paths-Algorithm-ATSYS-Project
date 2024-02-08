package com.adl.path.controller;

import com.adl.path.bean.Connection;
import com.adl.path.bean.Resp;
import com.adl.path.service.ConnService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RequestMapping("/conn")
//@RestController
public class ConnController {

    @Resource
    private ConnService connService;
    @GetMapping("/get/{id}")
    public Resp getConnection(@PathVariable int id){
        Connection edge = connService.getConn(id);
        return Resp.success(edge);
    }

}
