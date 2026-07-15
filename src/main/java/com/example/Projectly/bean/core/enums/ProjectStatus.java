package com.example.Projectly.bean.core.enums;

import com.example.Projectly.common.bean.BaseEnum;

/**
 * Enum: ProjectStatus
 */
public enum ProjectStatus implements BaseEnum {

    ACTIVE("Active"),
    ARCHIVED("Archived"),
    COMPLETED("Completed");

    private final String displayText;

    ProjectStatus(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public String getDisplayText() {
        return this.displayText;
    }

    /**
     * Find enum by display text (case-insensitive).
     *
     * @param text the display text to search for
     * @return the matching enum value, or null if not found
     */
    public static ProjectStatus fromDisplayText(String text) {
        if (text == null) return null;
        for (ProjectStatus value : values()) {
            if (value.displayText.equalsIgnoreCase(text)) {
                return value;
            }
        }
        return null;
    }
}
