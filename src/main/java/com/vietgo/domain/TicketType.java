package com.vietgo.domain;

/** Loai ve nhom phu trach: 2. Ve tau hoa, 3. Ve xe khach. */
public enum TicketType {
    TRAIN("Vé tàu hỏa", "TH", "bi-train-front"),
    BUS("Vé xe khách", "XK", "bi-bus-front");

    private final String label;
    private final String codePrefix;
    private final String icon;

    TicketType(String label, String codePrefix, String icon) {
        this.label = label;
        this.codePrefix = codePrefix;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getCodePrefix() { return codePrefix; }
    public String getIcon() { return icon; }
}
