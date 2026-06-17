package com.example.money_transfer_system.aspect;

import com.example.money_transfer_system.dto.LoginRequest;
import com.example.money_transfer_system.dto.RegisterRequest;
import com.example.money_transfer_system.dto.TransferRequest;
import com.example.money_transfer_system.dto.DepositRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.example.money_transfer_system.service..*(..))")
    public void serviceMethods() {
    }

    @Pointcut("execution(* com.example.money_transfer_system.controller..*(..))")
    public void controllerMethods() {
    }

    private static final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

    @Around("serviceMethods() || controllerMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        int depth = callDepth.get();
        callDepth.set(depth + 1);

        try {
            if (depth > 0) {
                return joinPoint.proceed();
            }

            // Use the actual target class to set the proper 'caller' in the logs
            Class<?> targetClass = joinPoint.getTarget().getClass();
            Logger targetLog = LoggerFactory.getLogger(targetClass);

            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();

            String humanMethod = camelCaseToWords(methodName);
            String context = extractContext(args);

            targetLog.info("Initiated '{}' operation{}", humanMethod, context);

            long startTime = System.currentTimeMillis();
            try {
                Object result = joinPoint.proceed();
                long executionTime = System.currentTimeMillis() - startTime;
                org.slf4j.MDC.put("duration", executionTime + " ms");
                targetLog.info("Successfully completed '{}' operation", humanMethod);
                org.slf4j.MDC.remove("duration");
                return result;
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                org.slf4j.MDC.put("duration", executionTime + " ms");
                targetLog.error("Failed '{}' operation due to error: {}",
                        humanMethod, e.getMessage());
                org.slf4j.MDC.remove("duration");
                throw e;
            }
        } finally {
            if (depth == 0) {
                callDepth.remove();
            } else {
                callDepth.set(depth);
            }
        }
    }

    private String camelCaseToWords(String camelCase) {
        if (camelCase == null || camelCase.isEmpty())
            return "";
        return camelCase.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"),
                " ").toLowerCase();
    }

    private String extractContext(Object[] args) {
        if (args == null || args.length == 0)
            return "";

        // Specific contextual identifiers
        for (Object arg : args) {
            if (arg == null)
                continue;

            if (arg instanceof String && ((String) arg).startsWith("Bearer ")) {
                continue; // Ignore authorization tokens entirely
            }

            if (arg instanceof LoginRequest) {
                return " for email " + ((LoginRequest) arg).getEmail();
            } else if (arg instanceof RegisterRequest) {
                return " for email " + ((RegisterRequest) arg).getEmail();
            } else if (arg instanceof TransferRequest) {
                return " of amount ₹" + ((TransferRequest) arg).getAmount() + " to account ID "
                        + ((TransferRequest) arg).getToAccountId();
            } else if (arg instanceof DepositRequest) {
                return " for account ID " + ((DepositRequest) arg).getAccountId();
            }
        }

        // Fallback for simple identifiers (like IDs passed as PathVariables)
        for (Object arg : args) {
            if (arg == null)
                continue;
            if (arg instanceof Long || arg instanceof Integer) {
                return " for ID " + arg;
            } else if (arg instanceof String) {
                String strArg = (String) arg;
                if (!strArg.startsWith("Bearer ") && !strArg.contains("password") && strArg.length() < 50) {
                    return " for '" + strArg + "'";
                }
            }
        }

        return "";
    }
}
