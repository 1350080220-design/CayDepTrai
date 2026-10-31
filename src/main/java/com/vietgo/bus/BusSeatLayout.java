package com.vietgo.bus;

import com.vietgo.domain.BusType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * So do ghe xe khach.
 * - Ghe ngoi 45 cho: 10 hang (2 | loi di | 2) + hang cuoi 5 ghe. Ma A01..A45.
 * - Giuong nam 36 cho: 2 tang, moi tang 6 hang x 3 day giuong. Tang duoi A01..A18, tang tren B01..B18.
 * - Phong nam cabin 24 phong: 2 tang, moi tang 6 hang x 2 day phong. Tang duoi A01..A12, tang tren B01..B12.
 * - Limousine 9 cho: hang 1-3 moi hang 2 ghe, hang cuoi 3 ghe. Ma L1..L9. Hang 1 la ghe VIP.
 * Trong danh sach hang, gia tri null la loi di.
 */
public final class BusSeatLayout {

    private BusSeatLayout() {
    }

    /** Mot tang xe: ten tang + cac hang ghe (null = loi di). */
    public record Deck(String name, List<List<String>> rows) {
    }

    public static List<Deck> decks(BusType type) {
        return switch (type) {
            case SEAT_45 -> List.of(new Deck("Khoang hành khách", seat45Rows()));
            case SLEEPER_36 -> List.of(new Deck("Tầng dưới", sleeperRows("A")), new Deck("Tầng trên", sleeperRows("B")));
            case CABIN_24 -> List.of(new Deck("Tầng dưới", cabinRows("A")), new Deck("Tầng trên", cabinRows("B")));
            case LIMOUSINE_9 -> List.of(new Deck("Khoang Limousine", limoRows()));
        };
    }

    private static List<List<String>> cabinRows(String deck) {
        List<List<String>> rows = new ArrayList<>();
        for (int r = 0; r < 6; r++) {
            int b = r * 2;
            rows.add(Arrays.asList(code(deck, b + 1), null, code(deck, b + 2)));
        }
        return rows;
    }

    private static List<List<String>> seat45Rows() {
        List<List<String>> rows = new ArrayList<>();
        for (int r = 0; r < 10; r++) {
            int b = r * 4;
            rows.add(Arrays.asList(code("A", b + 1), code("A", b + 2), null, code("A", b + 3), code("A", b + 4)));
        }
        rows.add(Arrays.asList(code("A", 41), code("A", 42), code("A", 43), code("A", 44), code("A", 45)));
        return rows;
    }

    private static List<List<String>> sleeperRows(String deck) {
        List<List<String>> rows = new ArrayList<>();
        for (int r = 0; r < 6; r++) {
            int b = r * 3;
            rows.add(Arrays.asList(code(deck, b + 1), null, code(deck, b + 2), null, code(deck, b + 3)));
        }
        return rows;
    }

    private static List<List<String>> limoRows() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("L1", null, "L2"));
        rows.add(Arrays.asList("L3", null, "L4"));
        rows.add(Arrays.asList("L5", null, "L6"));
        rows.add(Arrays.asList("L7", "L8", "L9"));
        return rows;
    }

    private static String code(String prefix, int n) {
        return prefix + (n < 10 ? "0" + n : String.valueOf(n));
    }

    public static List<String> allSeats(BusType type) {
        List<String> all = new ArrayList<>();
        for (Deck d : decks(type)) {
            for (List<String> row : d.rows()) {
                for (String s : row) {
                    if (s != null) all.add(s);
                }
            }
        }
        return all;
    }

    public static boolean exists(BusType type, String code) {
        return code != null && allSeats(type).contains(code);
    }

    /** He so gia theo vi tri: giuong/phong tang duoi +10%, ghe VIP hang dau limousine +15%. */
    public static double positionFactor(BusType type, String code) {
        if (type.isDoubleDeck() && code.startsWith("A")) return 1.10;
        if (type == BusType.LIMOUSINE_9 && (code.equals("L1") || code.equals("L2"))) return 1.15;
        return 1.0;
    }

    public static String positionNote(BusType type, String code) {
        if (type.isDoubleDeck()) return code.startsWith("A") ? "tầng dưới" : "tầng trên";
        if (type == BusType.LIMOUSINE_9 && (code.equals("L1") || code.equals("L2"))) return "hàng VIP";
        return null;
    }

    /** Tang cua cho (1 = tang duoi, 2 = tang tren); 0 voi xe mot tang. */
    public static int tierOf(BusType type, String code) {
        if (!type.isDoubleDeck()) return 0;
        return code.startsWith("A") ? 1 : 2;
    }

    public static String label(BusType type, String code) {
        String note = positionNote(type, code);
        return type.getUnit() + " " + code + (note == null ? "" : " (" + note + ")");
    }
}
