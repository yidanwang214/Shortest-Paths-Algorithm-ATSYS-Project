package com.adl.path.dao;


import com.adl.path.bean.Connection;
import com.adl.path.bean.ConnectionExt;

import java.util.List;

public interface ConnDao {
    Connection selectConn(int id);
    List<ConnectionExt> listAvailableConn();
}
