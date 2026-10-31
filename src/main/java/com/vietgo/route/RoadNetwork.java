package com.vietgo.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Mang duong bo don gian hoa (quoc lo 1A, cao toc va cac tuyen len Tay Nguyen, Tay Bac, mien Tay).
 * Moi nut la mot thanh pho hoac thi tran co toa do; cu ly canh = khoang cach duong chim bay x he so
 * duong bo. Lo trinh xe khach = duong di ngan nhat (Dijkstra) giua noi di va noi den.
 */
public final class RoadNetwork {

    private RoadNetwork() {
    }

    public record Node(String name, double lat, double lng) {
    }

    /** Duong bo thuc te dai hon duong chim bay khoang 20-25%. */
    private static final double ROAD_FACTOR = 1.22;

    private static final Map<String, Node> NODES = new LinkedHashMap<>();
    private static final Map<String, Map<String, Integer>> ADJ = new HashMap<>();

    static {
        // Thanh pho co ben xe khach (khop voi BusService.CITIES)
        node("Hà Nội", 21.0285, 105.8542);
        node("Hải Phòng", 20.8449, 106.6881);
        node("Hạ Long", 20.9560, 107.0480);
        node("Sa Pa", 22.3364, 103.8438);
        node("Ninh Bình", 20.2506, 105.9745);
        node("Vinh", 18.6796, 105.6813);
        node("Huế", 16.4637, 107.5909);
        node("Đà Nẵng", 16.0544, 108.2022);
        node("Hội An", 15.8801, 108.3380);
        node("Quy Nhơn", 13.7820, 109.2194);
        node("Nha Trang", 12.2388, 109.1967);
        node("Đà Lạt", 11.9404, 108.4583);
        node("Buôn Ma Thuột", 12.6667, 108.0383);
        node("Phan Thiết", 10.9289, 108.1021);
        node("Vũng Tàu", 10.3460, 107.0843);
        node("TP. Hồ Chí Minh", 10.7769, 106.7009);
        node("Cần Thơ", 10.0452, 105.7469);
        // Thi tran, nut giao tren duong
        node("Nội Bài", 21.2187, 105.8042);
        node("Việt Trì", 21.3227, 105.4019);
        node("Yên Bái", 21.7229, 104.9113);
        node("Lào Cai", 22.4856, 103.9707);
        node("Bắc Ninh", 21.1861, 106.0763);
        node("Hải Dương", 20.9373, 106.3146);
        node("Uông Bí", 21.0333, 106.7700);
        node("Phủ Lý", 20.5411, 105.9139);
        node("Thanh Hóa", 19.8067, 105.7852);
        node("Diễn Châu", 18.9800, 105.6000);
        node("Hà Tĩnh", 18.3428, 105.9057);
        node("Kỳ Anh", 18.0700, 106.3000);
        node("Đồng Hới", 17.4689, 106.6223);
        node("Đông Hà", 16.8163, 107.1003);
        node("Lăng Cô", 16.2500, 108.0700);
        node("Tam Kỳ", 15.5736, 108.4740);
        node("Quảng Ngãi", 15.1214, 108.8044);
        node("Bồng Sơn", 14.4300, 109.0100);
        node("Sông Cầu", 13.4500, 109.2200);
        node("Tuy Hòa", 13.0955, 109.3009);
        node("Ninh Hòa", 12.4917, 109.1259);
        node("Cam Ranh", 11.9214, 109.1591);
        node("Phan Rang", 11.5643, 108.9886);
        node("Cà Ná", 11.3300, 108.8700);
        node("Hàm Thuận Nam", 10.8600, 107.9000);
        node("Xuân Lộc", 10.9300, 107.4100);
        node("Dầu Giây", 10.9417, 107.1408);
        node("Biên Hòa", 10.9574, 106.8427);
        node("Long Thành", 10.7800, 106.9500);
        node("Bà Rịa", 10.4963, 107.1684);
        node("Định Quán", 11.1940, 107.3440);
        node("Bảo Lộc", 11.5480, 107.8070);
        node("Di Linh", 11.5800, 108.0700);
        node("Đức Trọng", 11.7500, 108.3700);
        node("Sông Pha", 11.8300, 108.7300);
        node("Đơn Dương", 11.7800, 108.5500);
        node("Khánh Vĩnh", 12.2700, 108.9000);
        node("Khánh Lê", 12.1300, 108.6600);
        node("Tân An", 10.5360, 106.4130);
        node("Trung Lương", 10.3600, 106.3600);
        node("Vĩnh Long", 10.2537, 105.9722);
        node("Đồng Xoài", 11.5349, 106.8832);
        node("Gia Nghĩa", 12.0042, 107.6907);
        node("M'Đrắk", 12.7000, 108.7700);

        // Tay Bac, Dong Bac
        road("Hà Nội", "Nội Bài", "Việt Trì", "Yên Bái", "Lào Cai", "Sa Pa");
        road("Hà Nội", "Bắc Ninh", "Hải Dương", "Uông Bí", "Hạ Long");
        road("Hà Nội", "Hải Phòng", "Hạ Long");
        road("Hải Dương", "Hải Phòng");
        // Quoc lo 1A / cao toc Bac - Nam
        road("Hà Nội", "Phủ Lý", "Ninh Bình", "Thanh Hóa", "Diễn Châu", "Vinh", "Hà Tĩnh", "Kỳ Anh",
                "Đồng Hới", "Đông Hà", "Huế", "Lăng Cô", "Đà Nẵng", "Tam Kỳ", "Quảng Ngãi", "Bồng Sơn",
                "Quy Nhơn", "Sông Cầu", "Tuy Hòa", "Ninh Hòa", "Nha Trang", "Cam Ranh", "Phan Rang", "Cà Ná",
                "Phan Thiết", "Hàm Thuận Nam", "Xuân Lộc", "Dầu Giây", "Biên Hòa", "TP. Hồ Chí Minh");
        road("Đà Nẵng", "Hội An", "Tam Kỳ");
        // Mien Dong, Tay Nguyen
        road("TP. Hồ Chí Minh", "Long Thành", "Bà Rịa", "Vũng Tàu");
        road("Long Thành", "Dầu Giây");
        road("Dầu Giây", "Định Quán", "Bảo Lộc", "Di Linh", "Đức Trọng", "Đà Lạt");
        road("Phan Rang", "Sông Pha", "Đơn Dương", "Đà Lạt");
        road("Nha Trang", "Khánh Vĩnh", "Khánh Lê", "Đà Lạt");
        road("TP. Hồ Chí Minh", "Đồng Xoài", "Gia Nghĩa", "Buôn Ma Thuột");
        road("Buôn Ma Thuột", "M'Đrắk", "Ninh Hòa");
        // Mien Tay
        road("TP. Hồ Chí Minh", "Tân An", "Trung Lương", "Vĩnh Long", "Cần Thơ");
    }

    private static void node(String name, double lat, double lng) {
        NODES.put(name, new Node(name, lat, lng));
    }

    /** Noi cac nut lien tiep thanh mot tuyen duong hai chieu. */
    private static void road(String... names) {
        for (int i = 0; i + 1 < names.length; i++) {
            Node a = require(names[i]), b = require(names[i + 1]);
            int km = (int) Math.round(Geo.distanceKm(a.lat(), a.lng(), b.lat(), b.lng()) * ROAD_FACTOR);
            ADJ.computeIfAbsent(a.name(), k -> new HashMap<>()).put(b.name(), km);
            ADJ.computeIfAbsent(b.name(), k -> new HashMap<>()).put(a.name(), km);
        }
    }

    private static Node require(String name) {
        Node n = NODES.get(name);
        if (n == null) throw new IllegalStateException("Thieu toa do cho nut " + name);
        return n;
    }

    public static boolean contains(String name) {
        return name != null && NODES.containsKey(name);
    }

    public static int edgeKm(Node a, Node b) {
        Integer km = ADJ.getOrDefault(a.name(), Map.of()).get(b.name());
        return km == null ? 0 : km;
    }

    /** Duong di ngan nhat (theo km) tu noi di den noi den; rong neu khong co. */
    public static List<Node> shortestPath(String from, String to) {
        if (!contains(from) || !contains(to) || from.equals(to)) return List.of();
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Map.Entry<String, Integer>> queue = new PriorityQueue<>(Map.Entry.comparingByValue());
        dist.put(from, 0);
        queue.add(Map.entry(from, 0));
        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> cur = queue.poll();
            String u = cur.getKey();
            if (cur.getValue() > dist.getOrDefault(u, Integer.MAX_VALUE)) continue;
            if (u.equals(to)) break;
            for (Map.Entry<String, Integer> e : ADJ.getOrDefault(u, Map.of()).entrySet()) {
                int nd = cur.getValue() + e.getValue();
                if (nd < dist.getOrDefault(e.getKey(), Integer.MAX_VALUE)) {
                    dist.put(e.getKey(), nd);
                    prev.put(e.getKey(), u);
                    queue.add(Map.entry(e.getKey(), nd));
                }
            }
        }
        if (!dist.containsKey(to)) return List.of();
        List<Node> path = new ArrayList<>();
        for (String at = to; at != null; at = prev.get(at)) path.add(NODES.get(at));
        Collections.reverse(path);
        return path;
    }
}
