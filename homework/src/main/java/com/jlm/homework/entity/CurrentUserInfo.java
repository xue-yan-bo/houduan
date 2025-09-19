package com.jlm.homework.entity;

import lombok.Data;

import java.util.List;

public class CurrentUserInfo {
    private Long userId;
    private String userName;
    private String nickName;
    private String userUuid;
    private boolean isAdmin;
    private List<String> roles;
    private String currentRole;
    private String teacherName;

    public CurrentUserInfo(Long userId, String userName, String nickName, String userUuid,
                           boolean isAdmin, List<String> roles, String currentRole, String teacherName) {
        this.userId = userId;
        this.userName = userName;
        this.nickName = nickName;
        this.userUuid = userUuid;
        this.isAdmin = isAdmin;
        this.roles = roles;
        this.currentRole = currentRole;
        this.teacherName = teacherName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
}
