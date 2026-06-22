package com.horovod.hub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "horovod")
public class HorovodProperties {

    @Value("${horovod.admin-email}")
    private String adminEmail;

    @Value("${horovod.admin-password-fallback}")
    private String adminPasswordFallback;

    @Value("${horovod.otp-bypass-codes}")
    private String otpBypassCodes;
    ;
    private int maxConcurrentPerSlot = 2;
    private int maxLogEntries = 50;
    private int calendarStartHour = 8;
    private int calendarEndHour = 24;
    private boolean emailEnabled = false;

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPasswordFallback() {
        return adminPasswordFallback;
    }

    public void setAdminPasswordFallback(String adminPasswordFallback) {
        this.adminPasswordFallback = adminPasswordFallback;
    }

    public String getOtpBypassCodes() {
        return otpBypassCodes;
    }

    public void setOtpBypassCodes(String otpBypassCodes) {
        this.otpBypassCodes = otpBypassCodes;
    }

    public int getMaxConcurrentPerSlot() {
        return maxConcurrentPerSlot;
    }

    public void setMaxConcurrentPerSlot(int maxConcurrentPerSlot) {
        this.maxConcurrentPerSlot = maxConcurrentPerSlot;
    }

    public int getMaxLogEntries() {
        return maxLogEntries;
    }

    public void setMaxLogEntries(int maxLogEntries) {
        this.maxLogEntries = maxLogEntries;
    }

    public int getCalendarStartHour() {
        return calendarStartHour;
    }

    public void setCalendarStartHour(int calendarStartHour) {
        this.calendarStartHour = calendarStartHour;
    }

    public int getCalendarEndHour() {
        return calendarEndHour;
    }

    public void setCalendarEndHour(int calendarEndHour) {
        this.calendarEndHour = calendarEndHour;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isAdminEmail(String email) {
        return adminEmail.equalsIgnoreCase(email != null ? email.trim() : "");
    }

    public boolean isOtpBypass(String code) {
        if (code == null) {
            return false;
        }
        for (String bypass : otpBypassCodes.split(",")) {
            if (bypass.trim().equals(code.trim())) {
                return true;
            }
        }
        return false;
    }
}
