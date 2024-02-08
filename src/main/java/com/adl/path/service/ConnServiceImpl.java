package com.adl.path.service;

import com.adl.path.bean.Connection;
import com.adl.path.bean.ConnectionExt;
import com.adl.path.dao.ConnDao;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ConnServiceImpl implements ConnService {
    @Resource
    private ConnDao connDao;
    @Override
    public Connection getConn(int id) {
        return connDao.selectConn(id);
    }

    @Override
    public Connection selectConn(int id) {
        return connDao.selectConn(id);
    }

    @Override
    public List<ConnectionExt> listAvailableConn() {
        return connDao.listAvailableConn();
    }
}
