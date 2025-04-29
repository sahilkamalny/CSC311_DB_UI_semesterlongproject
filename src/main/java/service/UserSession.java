package service;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.prefs.Preferences;

public class UserSession {
    private static volatile UserSession instance;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final String userName;
    private final String password;
    private final String privileges;
    private static final Preferences userPreferences = Preferences.userRoot().node("com.studentreg.preferences");

    private UserSession(String userName, String password, String privileges) {
        this.userName = userName;
        this.password = password;
        this.privileges = privileges;
        saveToPreferences();
    }

    private void saveToPreferences() {
        userPreferences.put("USERNAME", userName);
        userPreferences.put("PASSWORD", password);
        userPreferences.put("PRIVILEGES", privileges);
    }

    public static UserSession getInstance(String userName, String password, String privileges) {
        UserSession result = instance;
        if (result == null) {
            lock.writeLock().lock();
            try {
                if (instance == null) {
                    instance = new UserSession(userName, password, privileges);
                }
                result = instance;
            } finally {
                lock.writeLock().unlock();
            }
        }
        return result;
    }

    public static UserSession getInstance(String userName, String password) {
        return getInstance(userName, password, "NONE");
    }

    public String getUserName() {
        lock.readLock().lock();
        try {
            return userName;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getPassword() {
        lock.readLock().lock();
        try {
            return password;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getPrivileges() {
        lock.readLock().lock();
        try {
            return privileges;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void cleanUserSession() {
        lock.writeLock().lock();
        try {
            userPreferences.remove("USERNAME");
            userPreferences.remove("PASSWORD");
            userPreferences.remove("PRIVILEGES");
            instance = null;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
