package com.licenta.backend.profile.dto;

import java.util.List;

public class UserProfileDto {
    public Long userId;
    public String email;
    public String firstName;
    public String lastName;
    public Long levelId;
    public String levelName;
    public Long positionId;
    public String positionName;
    public List<String> roles;
}
