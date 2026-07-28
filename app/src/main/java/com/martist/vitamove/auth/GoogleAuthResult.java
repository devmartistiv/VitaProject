package com.martist.vitamove.auth;


public class GoogleAuthResult {
    private final String responseJson;
    private final boolean isNewUser;
    private final String userId;
    private final String email;
    private final String displayName;

    public GoogleAuthResult(String responseJson, boolean isNewUser, String userId, String email, String displayName) {
        this.responseJson = responseJson;
        this.isNewUser = isNewUser;
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }
}
