package com.example.Projectly.bean.core.enums;

import com.example.Projectly.common.bean.BaseEnum;

/**
 * Enum: TaskPriority
 */
public enum TaskPriority implements BaseEnum {

    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");

    private final String displayText;

    TaskPriority(String displayText) {
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
    public static TaskPriority fromDisplayText(String text) {
        if (text == null) return null;
        for (TaskPriority value : values()) {
            if (value.displayText.equalsIgnoreCase(text)) {
                return value;
            }
        }
        return null;
    }
}
