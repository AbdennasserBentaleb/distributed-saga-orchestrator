package com.saga.common.enums;

public enum SagaStatus {
    PENDING,
    FLIGHT_BOOKED,
    HOTEL_BOOKED,
    CAR_FAILED,
    CANCELLING_HOTEL,
    CANCELLING_FLIGHT,
    COMPENSATED,
    COMPLETED,
    /** Terminal state: compensation itself failed after exhausting all retries. Requires manual intervention. */
    FAILED
}
