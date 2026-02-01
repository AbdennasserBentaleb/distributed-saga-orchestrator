package com.saga.orchestrator.statemachine;

import com.saga.common.enums.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
public class SagaStateMachineConfig extends EnumStateMachineConfigurerAdapter<SagaStatus, SagaStateMachineEvent> {

    private final SagaActions sagaActions;

    @Override
    public void configure(StateMachineStateConfigurer<SagaStatus, SagaStateMachineEvent> states) throws Exception {
        states.withStates()
                .initial(SagaStatus.PENDING)
                .states(EnumSet.allOf(SagaStatus.class))
                .end(SagaStatus.COMPLETED)
                .end(SagaStatus.COMPENSATED)
                .end(SagaStatus.FAILED);  // Terminal state when compensation itself fails
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<SagaStatus, SagaStateMachineEvent> transitions) throws Exception {
        transitions
                // Flight success path
                .withExternal()
                .source(SagaStatus.PENDING).target(SagaStatus.FLIGHT_BOOKED)
                .event(SagaStateMachineEvent.FLIGHT_SUCCESS)
                .action(sagaActions.dispatchHotelCommand())
                .and()
                // Hotel success path
                .withExternal()
                .source(SagaStatus.FLIGHT_BOOKED).target(SagaStatus.HOTEL_BOOKED)
                .event(SagaStateMachineEvent.HOTEL_SUCCESS)
                .action(sagaActions.dispatchCarCommand())
                .and()
                // Car success path
                .withExternal()
                .source(SagaStatus.HOTEL_BOOKED).target(SagaStatus.COMPLETED)
                .event(SagaStateMachineEvent.CAR_SUCCESS)
                .action(sagaActions.logCompleted())
                .and()
                
                // Failures
                .withExternal()
                .source(SagaStatus.PENDING).target(SagaStatus.COMPENSATED)
                .event(SagaStateMachineEvent.FLIGHT_FAILED)
                .action(sagaActions.logCompensated())
                .and()
                .withExternal()
                .source(SagaStatus.FLIGHT_BOOKED).target(SagaStatus.CANCELLING_FLIGHT)
                .event(SagaStateMachineEvent.HOTEL_FAILED)
                .action(sagaActions.dispatchCancelFlightCommand())
                .and()
                .withExternal()
                .source(SagaStatus.HOTEL_BOOKED).target(SagaStatus.CANCELLING_HOTEL)
                .event(SagaStateMachineEvent.CAR_FAILED)
                .action(sagaActions.dispatchCancelHotelCommand())
                .and()
                
                // Compensation success paths
                .withExternal()
                .source(SagaStatus.CANCELLING_HOTEL).target(SagaStatus.CANCELLING_FLIGHT)
                .event(SagaStateMachineEvent.HOTEL_SUCCESS) // hotel cancelled successfully
                .action(sagaActions.dispatchCancelFlightCommand())
                .and()
                .withExternal()
                .source(SagaStatus.CANCELLING_FLIGHT).target(SagaStatus.COMPENSATED)
                .event(SagaStateMachineEvent.FLIGHT_SUCCESS) // flight cancelled successfully
                .action(sagaActions.logCompensated())
                .and()

                // Compensation failure paths — prevent infinite stuck-saga loops.
                // If a cancellation itself fails (e.g., downstream service is unavailable),
                // the StuckSagaSweeper will re-trigger. After max retries, the saga
                // moves to FAILED so operations teams can investigate via the DLQ.
                .withExternal()
                .source(SagaStatus.CANCELLING_HOTEL).target(SagaStatus.FAILED)
                .event(SagaStateMachineEvent.HOTEL_FAILED)
                .action(sagaActions.logCompensationFailed())
                .and()
                .withExternal()
                .source(SagaStatus.CANCELLING_FLIGHT).target(SagaStatus.FAILED)
                .event(SagaStateMachineEvent.FLIGHT_FAILED)
                .action(sagaActions.logCompensationFailed());
    }
}
