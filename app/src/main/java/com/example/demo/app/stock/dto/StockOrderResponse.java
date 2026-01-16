package com.example.demo.app.stock.dto;

public class StockOrderResponse {
    private String rt_cd;
    private String msg_cd;
    private String msg1;

    // getters and setters

    public String getRt_cd() {
        return rt_cd;
    }

    public void setRt_cd(String rt_cd) {
        this.rt_cd = rt_cd;
    }

    public String getMsg_cd() {
        return msg_cd;
    }

    public void setMsg_cd(String msg_cd) {
        this.msg_cd = msg_cd;
    }

    public String getMsg1() {
        return msg1;
    }

    public void setMsg1(String msg1) {
        this.msg1 = msg1;
    }
}
