package com.vietgo.common;

/** Cac bieu thuc chinh quy dung chung cho validation. */
public final class Patterns {

    private Patterns() {
    }

    /** So dien thoai di dong Viet Nam: 0 hoac +84, dau so 3/5/7/8/9, tong 10 so. */
    public static final String VN_PHONE = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$";

    /** Ho ten: chu cai (co dau) va khoang trang. */
    public static final String PERSON_NAME = "^[\\p{L}][\\p{L} .'-]{1,59}$";

    /** Can cuoc cong dan 12 so. */
    public static final String CCCD = "^[0-9]{12}$";

    /** Ho chieu Viet Nam: 1 chu cai + 7 so. */
    public static final String PASSPORT = "^[A-Z][0-9]{7}$";

    /** The hoc sinh, sinh vien: 6-15 ky tu chu/so. */
    public static final String STUDENT_CARD = "^[A-Za-z0-9]{6,15}$";

    /** Mat khau: >= 8 ky tu, co chu hoa, chu thuong va chu so. */
    public static final String STRONG_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$";

    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        String p = phone.replaceAll("[\\s.-]", "");
        if (p.startsWith("+84")) p = "0" + p.substring(3);
        return p;
    }

    public static String normalizeName(String name) {
        if (name == null) return null;
        return name.trim().replaceAll("\\s+", " ");
    }
}
