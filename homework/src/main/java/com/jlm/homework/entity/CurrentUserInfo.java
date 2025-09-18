package com.jlm.homework.entity;

import lombok.Data;

import java.util.List;

@Data
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
}
