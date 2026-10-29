package com.vietgo.common;

/** Loi nghiep vu - thong bao duoc hien thi truc tiep cho nguoi dung. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
