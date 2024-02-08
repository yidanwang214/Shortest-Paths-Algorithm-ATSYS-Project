package com.adl.path.bean;

import lombok.Data;

@Data
public class Resp {
    private static int SUCC = 1;
    private static int FAIL = 0;
    private int code;
    private String msg;
    private Object data;

    public Resp(int code, String msg, Object data) {
        this.code=code;
        this.msg=msg;
        this.data=data;
    }

    public static Resp success(Object data){
        return new Resp(SUCC,"success",data);
    }

    public static Resp fail(String msg) {
        return new Resp(FAIL,msg,null);
    }
}
