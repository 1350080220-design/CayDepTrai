package com.vietgo.domain;

/** Phuong thuc thanh toan (mo phong, khong luu thong tin the). */
public enum PaymentMethod {
    BANK_CARD("Thẻ ATM / Napas", "bi-credit-card-2-front"),
    E_WALLET("Ví điện tử (MoMo, ZaloPay)", "bi-wallet2"),
    BANK_TRANSFER("Chuyển khoản / VietQR", "bi-qr-code-scan"),
    INTL_CARD("Thẻ quốc tế Visa / Mastercard", "bi-credit-card");

    private final String label;
    private final String icon;

    PaymentMethod(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
}
