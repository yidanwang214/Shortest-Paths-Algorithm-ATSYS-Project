package com.adl.path.service;


import com.adl.path.bean.Connection;
import com.adl.path.bean.ConnectionExt;

import java.util.List;

public interface ConnService {
    Connection getConn(int id);

    Connection selectConn(int id);

    List<ConnectionExt> listAvailableConn();
}
