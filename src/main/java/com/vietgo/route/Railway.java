package com.vietgo.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tuyen duong sat Thong Nhat Ha Noi - Sai Gon: ly trinh (km tinh tu ga Ha Noi), toa do va thoi gian
 * dung do du kien tai tung ga. Cac diem "hinh dang" (stop = false) chi dung de ve duong ray uon theo
 * bo bien tren ban do, tau khong dung o do.
 */
public final class Railway {

    private Railway() {
    }

    public record Station(String name, int km, double lat, double lng, int dwellMinutes, boolean stop) {
    }

    public static final List<Station> LINE = List.of(
            station("Hà Nội", 0, 21.0245, 105.8412, 0),
            station("Phủ Lý", 56, 20.5450, 105.9130, 3),
            station("Nam Định", 87, 20.4280, 106.1690, 5),
            station("Ninh Bình", 115, 20.2540, 105.9750, 5),
            station("Thanh Hóa", 175, 19.8030, 105.7760, 8),
            station("Vinh", 319, 18.6830, 105.6730, 12),
            station("Đồng Lê", 436, 17.8850, 106.0260, 3),
            station("Đồng Hới", 522, 17.4660, 106.6000, 8),
            station("Đông Hà", 622, 16.8170, 107.0980, 5),
            station("Huế", 688, 16.4560, 107.5780, 12),
            shape(16.3000, 107.8700),
            shape(16.2440, 108.0600),
            shape(16.1900, 108.1300),
            station("Đà Nẵng", 791, 16.0710, 108.2090, 15),
            station("Tam Kỳ", 865, 15.5700, 108.4700, 3),
            station("Quảng Ngãi", 928, 15.1200, 108.7920, 5),
            station("Bồng Sơn", 1017, 14.4300, 109.0000, 3),
            station("Quy Nhơn (Diêu Trì)", 1096, 13.8030, 109.1500, 10),
            shape(13.4500, 109.2200),
            station("Tuy Hòa", 1198, 13.0950, 109.3000, 5),
            station("Ninh Hòa", 1281, 12.4900, 109.1300, 3),
            station("Nha Trang", 1315, 12.2480, 109.1840, 12),
            shape(11.9200, 109.1500),
            station("Tháp Chàm", 1408, 11.5880, 108.9450, 5),
            shape(11.3300, 108.8500),
            station("Phan Thiết", 1551, 10.9700, 108.0600, 5),
            shape(10.9300, 107.4000),
            station("Biên Hòa", 1697, 10.9460, 106.8180, 3),
            station("Sài Gòn", 1726, 10.7820, 106.6770, 0));

    private static Station station(String name, int km, double lat, double lng, int dwell) {
        return new Station(name, km, lat, lng, dwell, true);
    }

    private static Station shape(double lat, double lng) {
        return new Station(null, -1, lat, lng, 0, false);
    }

    private static int indexOf(String name) {
        if (name == null) return -1;
        for (int i = 0; i < LINE.size(); i++) {
            if (name.equals(LINE.get(i).name())) return i;
        }
        return -1;
    }

    public static boolean contains(String name) {
        return indexOf(name) >= 0;
    }

    /** Cu ly duong sat giua hai ga (km), -1 neu khong co ga tren tuyen. */
    public static int distanceKm(String from, String to) {
        int a = indexOf(from), b = indexOf(to);
        if (a < 0 || b < 0) return -1;
        return Math.abs(LINE.get(a).km() - LINE.get(b).km());
    }

    /** Doan tuyen tu ga di den ga den (gom ca hai dau), theo chieu tau chay; rong neu khong xac dinh. */
    public static List<Station> segment(String from, String to) {
        int a = indexOf(from), b = indexOf(to);
        if (a < 0 || b < 0 || a == b) return List.of();
        List<Station> seg = new ArrayList<>(LINE.subList(Math.min(a, b), Math.max(a, b) + 1));
        if (a > b) Collections.reverse(seg);
        return seg;
    }
}
