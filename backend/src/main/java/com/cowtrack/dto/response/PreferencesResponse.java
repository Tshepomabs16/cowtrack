package com.cowtrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesResponse {
    private String theme;
    private String language;
    private String timezone;
    private String units;
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean alertNotifications;
    private Boolean weeklyReports;
}
