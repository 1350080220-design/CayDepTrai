package com.vietgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * VietGo - Ung dung web quan ly dat ve du lich.
 * De 5 - Nhom phu trach: 2. Ve tau hoa va 3. Ve xe khach.
 * Cong nghe: Spring Boot, Spring Web MVC, Spring Data JPA, Spring Security, Thymeleaf, SQL Server.
 */
@SpringBootApplication
@EnableScheduling
public class VietGoApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(VietGoApplication.class, args);
    }
}
