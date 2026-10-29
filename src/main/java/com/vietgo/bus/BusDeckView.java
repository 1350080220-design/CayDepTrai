package com.vietgo.bus;

import com.vietgo.common.SeatView;

import java.util.List;

/** So do mot tang xe de hien thi; phan tu null trong hang la loi di. */
public class BusDeckView {

    private final String name;
    private final List<List<SeatView>> rows;

    public BusDeckView(String name, List<List<SeatView>> rows) {
        this.name = name;
        this.rows = rows;
    }

    public String getName() { return name; }
    public List<List<SeatView>> getRows() { return rows; }
}
