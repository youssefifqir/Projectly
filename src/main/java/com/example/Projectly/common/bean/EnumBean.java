package com.example.Projectly.common.bean;

import java.io.Serializable;

/**
 * Generic DTO for exposing enum values to the frontend.
 * Can be used to serialize enum values into a standard {id, label} format.
 */
public class EnumBean implements Serializable {

    private Integer id;
    private String label;

    public EnumBean() {
    }

    public EnumBean(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * Creates an EnumBean from a BaseEnum value.
     *
     * @param ordinal the ordinal value of the enum constant
     * @param baseEnum the BaseEnum instance
     * @return a new EnumBean
     */
    public static EnumBean fromBaseEnum(int ordinal, BaseEnum baseEnum) {
        return new EnumBean(ordinal, baseEnum.getDisplayText());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
