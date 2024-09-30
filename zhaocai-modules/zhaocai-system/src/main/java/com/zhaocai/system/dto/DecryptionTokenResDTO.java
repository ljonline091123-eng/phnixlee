package com.zhaocai.system.dto;

import java.util.Set;

public class DecryptionTokenResDTO {


    private String msg;
    private int code;
    private Dept dept;
    private String message;
    private User user;

    private Set<String> permission;
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public String getMsg() {
        return msg;
    }

    public void setCode(int code) {
        this.code = code;
    }
    public int getCode() {
        return code;
    }

    public void setDept(Dept dept) {
        this.dept = dept;
    }
    public Dept getDept() {
        return dept;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
    }


    public Set<String> getPermission() {
        return permission;
    }

    public void setPermission(Set<String> permission) {
        this.permission = permission;
    }
}
