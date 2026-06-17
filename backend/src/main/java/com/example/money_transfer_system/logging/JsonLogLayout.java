package com.example.money_transfer_system.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonLogLayout extends LayoutBase<ILoggingEvent> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss z");

    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> logMap = new LinkedHashMap<>();

        logMap.put("message", event.getFormattedMessage());
        logMap.put("level", event.getLevel().toString());
        logMap.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp())
                .atZone(ZoneId.systemDefault()).format(formatter));

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc.containsKey("method") && !mdc.get("method").isEmpty()) {
            logMap.put("method", mdc.get("method"));
        }
        if (mdc.containsKey("endpoint") && !mdc.get("endpoint").isEmpty()) {
            logMap.put("endpoint", mdc.get("endpoint"));
        }
        if (mdc.containsKey("traceId") && !mdc.get("traceId").isEmpty()) {
            logMap.put("traceId", mdc.get("traceId"));
        }

        // Use the logger name as caller
        logMap.put("caller", event.getLoggerName());

        if (mdc.containsKey("duration") && !mdc.get("duration").isEmpty()) {
            logMap.put("duration", mdc.get("duration"));
        }

        try {
            return mapper.writeValueAsString(logMap) + "\n";
        } catch (JsonProcessingException e) {
            return event.getFormattedMessage() + "\n";
        }
    }
}
