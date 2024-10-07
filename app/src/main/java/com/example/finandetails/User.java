package com.example.finandetails;

public class User {
    private String name, profession, email, password;
//    private String coverphoto;
//    private String buildNumber, modelNumber, androidVersion;
//    private String bio;
//    private int friendsCount;
//    private long coins;

    private String userID;
//    private String profileimage;

    public User(String name, String profession, String email, String password,
                String buildNumber, String modelNumber, String androidVersion) {
        this.name = name;
        this.profession = profession;
        this.email = email;
        this.password = password;
//        this.coins = coins;
//        this.buildNumber = buildNumber;
//        this.modelNumber = modelNumber;
//        this.androidVersion = androidVersion;
        // this.phoneNumber = phoneNumber;
    }

    public User() {

    }

//    public String getBuildNumber() {
//        return buildNumber;
//    }
//
//    public void setBuildNumber(String buildNumber) {
//        this.buildNumber = buildNumber;
//    }
//
//    public String getModelNumber() {
//        return modelNumber;
//    }
//
//    public void setModelNumber(String modelNumber) {
//        this.modelNumber = modelNumber;
//    }
//
//    public String getAndroidVersion() {
//        return androidVersion;
//    }
//
//    public void setAndroidVersion(String androidVersion) {
//        this.androidVersion = androidVersion;
//    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

//    public String getProfileimage() {
//        return profileimage;
//    }
//
//    public void setProfileimage(String profileimage) {
//        this.profileimage = profileimage;
//    }
//
//    public String getCoverphoto() {
//        return coverphoto;
//    }
//
//    public void setCoverphoto(String coverphoto) {
//        this.coverphoto = coverphoto;
//    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

//    public int getFriendsCount() {
//        return friendsCount;
//    }
//
//    public void setFriendsCount(int friendsCount) {
//        this.friendsCount = friendsCount;
//    }
//
//    public String getBio() {
//        return bio;
//    }
//
//    public void setBio(String bio) {
//        this.bio = bio;
//    }
//}