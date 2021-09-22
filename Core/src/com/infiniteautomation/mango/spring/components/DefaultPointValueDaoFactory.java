/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.pointvalue.PointValueDaoFactory;
import com.serotonin.log.LogStopWatch;
import com.serotonin.m2m2.db.PointValueDaoDefinition;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.vo.DataPointVO;

@Component
public class DefaultPointValueDaoFactory implements PointValueDaoFactory {
    private final List<PointValueDaoDefinition> definitions;
    private final boolean useMetrics;
    private final Environment env;
    private final Set<String> noLogMethods = Set.of("savePointValueSync", "savePointValueAsync");

    public DefaultPointValueDaoFactory(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") List<PointValueDaoDefinition> definitions,
                                       @Value("${db.useMetrics}") boolean useMetrics, Environment env) {
        this.definitions = definitions;
        this.useMetrics = useMetrics;
        this.env = env;
    }

    @Override
    public PointValueDao getPointValueDao() {
        PointValueDaoDefinition definition = definitions.stream().findFirst().orElseThrow();
        PointValueDao dao = definition.getPointValueDao();
        return useMetrics ? createMetricsPointValueDao(dao) : dao;
    }

    protected PointValueDao createMetricsPointValueDao(PointValueDao delegate) {
        Class<?> clazz = delegate.getClass();
        return (PointValueDao) Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), (proxy, method, args) -> {
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
