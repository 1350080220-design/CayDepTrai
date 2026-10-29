package com.vietgo.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Tac vu nen: moi 30 giay giai phong ghe cua cac don giu cho qua han. */
@Component
public class HoldExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryJob.class);

    private final BookingService bookingService;

    public HoldExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    public void releaseExpiredHolds() {
        try {
            int n = bookingService.expireHolds(LocalDateTime.now());
            if (n > 0) log.info("Da giai phong {} don giu cho qua han", n);
        } catch (Exception e) {
            log.warn("Loi khi giai phong don qua han: {}", e.getMessage());
        }
    }
}
