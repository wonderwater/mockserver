package com.example.mockserver.entity;

public record Resp<T>(Integer code, String msg, T body) {

    private static Resp succ = new Resp(200, "成功", null);

    public static Resp fail(String msg) {
        return new Resp(500, msg, null);
    }
    public static Resp succ(String msg) {
        return new Resp(200, msg, null);
    }
    public static Resp succ() {
        return succ;
    }
    public static Resp newResp(boolean succFlag, String msg) {
        if (succFlag) {
            return succ(msg);
        } else {
            return fail(msg);
        }
    }
    public static Resp newResp(boolean succFlag) {
        return newResp(succFlag, "");
    }

    public static Resp succBody(Object body) {
        return new Resp(succ.code, succ.msg, body);
    }


}
