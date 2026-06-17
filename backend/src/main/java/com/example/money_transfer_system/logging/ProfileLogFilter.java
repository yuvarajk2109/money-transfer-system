package com.example.money_transfer_system.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class ProfileLogFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getFormattedMessage() != null && event.getFormattedMessage().contains("No active profile set")) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }
}
