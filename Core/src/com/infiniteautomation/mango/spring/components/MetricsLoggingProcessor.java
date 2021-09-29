/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.serotonin.log.LogStopWatch;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * Proxies PointValueDao and adds logging. This could be expanded to proxy other classes by looking for an
 * annotation.
 */
@Component
public class MetricsLoggingProcessor implements BeanPostProcessor {

    private final boolean useMetrics;
    private final Environment env;
    private final Set<String> noLogMethods = Set.of("savePointValueSync", "savePointValueAsync");

    public MetricsLoggingProcessor(@Value("${db.useMetrics}") boolean useMetrics, Environment env) {
        this.useMetrics = useMetrics;
        this.env = env;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (useMetrics && bean instanceof PointValueDao) {
            return createMetricsPointValueDao((PointValueDao) bean);
        }
        return bean;
    }

    protected PointValueDao createMetricsPointValueDao(PointValueDao delegate) {
        Class<? extends PointValueDao> clazz = delegate.getClass();
        return (PointValueDao) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {PointValueDao.class}, (proxy, method, args) -> {
            try {
                if (noLogMethods.contains(method.getName())) {
                    return method.invoke(delegate, args);
                }

                long metricsThreshold = env.getRequiredProperty("db.metricsThreshold", long.class);
                LogStopWatch stopWatch = new LogStopWatch(LoggerFactory.getLogger(clazz));
                Object result = method.invoke(delegate, args);
                stopWatch.stop(() -> metricsLogLine(method, args), metricsThreshold);
                return result;
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    protected String metricsLogLine(Method method, Object[] args) {
        return String.format("%s(%s) (%s)",
                method.getName(),
                Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")),
                Arrays.stream(args).map(this::metricsFormatArg).collect(Collectors.joining(", ")));
    }

    protected String metricsFormatArg(Object arg) {
        if (arg == null) return "null";
        if (arg instanceof DataPointVO) return ((DataPointVO) arg).getXid();
        if (arg instanceof Collection) {
            Collection<?> collection = (Collection<?>) arg;
            if (collection.size() > 10) return "[" + collection.size() + "]";
            else return "[" + collection.stream().map(this::metricsFormatArg).collect(Collectors.joining(", ")) + "]";
        }
        return arg.toString();
    }
}
