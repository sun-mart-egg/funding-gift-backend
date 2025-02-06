package com.d201.fundingift._common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExecutionTimeAspect {
    @Around("execution(* com..fundingift..service.*.*(..))")
    public Object logServiceMethodExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        return logExecutionTime(joinPoint, "\u001B[34m");
    }

    private Object logExecutionTime(ProceedingJoinPoint joinPoint, String color) throws Throwable {

        long startTime = System.currentTimeMillis();
        log.info("{}[{}.{}] start", color, joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());

        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        log.info("{}[{}.{}] end, 실행 시간: {}ms", color,
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(), executionTime);

        return result;
    }
}
