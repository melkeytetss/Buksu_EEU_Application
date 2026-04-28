package com.example.buksu_eeu;

public class UserModel {
    private String uid;
    private String username;
    private String email;
    private String phone;
    private String role;
    private String profilePhoto;

    public UserModel() {}

    public UserModel(String uid, String username, String email, String phone, String role, String profilePhoto) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.profilePhoto = profilePhoto;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
}
