package com.saga.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * The SpEL expression to determine the lock key.
     * Example: "#command.sagaId"
     */
    String keyPrefix();
    String keyExpression() default "";
    long waitTime() default 5;
    long leaseTime() default 10;
}
