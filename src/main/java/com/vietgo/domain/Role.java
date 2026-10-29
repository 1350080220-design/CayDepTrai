package com.vietgo.domain;

/** Vai tro nguoi dung. */
public enum Role {
    USER("Khách hàng"),
    ADMIN("Quản trị viên");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }
}
