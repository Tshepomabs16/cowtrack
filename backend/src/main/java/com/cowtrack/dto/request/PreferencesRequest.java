package com.cowtrack.dto.request;

import lombok.Data;

/** All fields optional: only those present are applied. */
@Data
public class PreferencesRequest {
    private String theme;
    private String language;
    private String timezone;
    private String units;
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean alertNotifications;
    private Boolean weeklyReports;
}
