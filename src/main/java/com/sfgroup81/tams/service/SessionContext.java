package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.User;

public final class SessionContext {
    private static User currentUser;

    private SessionContext() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
