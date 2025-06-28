package com.yash.notification.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum AddressType {
    HOME,
    WORK,
    BILLING,
    SHIPPING
}