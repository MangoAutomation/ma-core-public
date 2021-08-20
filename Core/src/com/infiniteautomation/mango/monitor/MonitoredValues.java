/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.monitor;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

/**
 * List of Values Monitored for Mango
 *
 * @author Matthew Lohbihler, Terry Packer
 */
public class MonitoredValues implements DynamicMBean {

    private final Map<String, ValueMonitor<?>> monitors = new ConcurrentHashMap<>();

    public MonitoredValues() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName = new ObjectName("com.radixiot.mango:name=" + MonitoredValues.class.getSimpleName());
            platformMBeanServer.registerMBean(this, objectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds monitor, if ID already exists it will be replaced by the new monitor.
     * @param monitor
     */
    private void add(ValueMonitor<?> monitor) {
        Objects.requireNonNull(monitor);
        monitors.put(monitor.getId(), monitor);
    }

    /**
     * Remove monitor with given id
     * @param id
     * @return the removed monitor if it existed
     */
    public ValueMonitor<?> remove(String id) {
        return monitors.remove(Objects.requireNonNull(id));
    }

    /**
     * @return all monitors sorted by id
     */
    public List<ValueMonitor<?>> getMonitors() {
        List<ValueMonitor<?>> list = new ArrayList<>(monitors.values());
        Collections.sort(list, (a, b) -> a.getId().compareTo(b.getId()));
        return list;
    }

    /**
     * Get the monitor with given Id
     * @param id
     * @return
     */
    public ValueMonitor<?> getMonitor(String id) {
        return monitors.get(Objects.requireNonNull(id));
    }

    public <T> ValueMonitorBuilder<T> create(String id) {
        return new ValueMonitorBuilder<T>(id);
    }

    public class ValueMonitorBuilder<T> {
        private final String id;
        private TranslatableMessage name;
        private T value;
        private boolean uploadToStore;
        private Function<Long, T> function;
        private Collection<ValueMonitor<?>> addTo;

        private ValueMonitorBuilder(String id) {
            this.id = id;
        }
        public ValueMonitorBuilder<T> name(TranslatableMessage name) {
            this.name = name;
            return this;
        }
        public ValueMonitorBuilder<T> value(T value) {
            this.value = value;
            return this;
        }
        public ValueMonitorBuilder<T> uploadToStore(boolean uploadToStore) {
            this.uploadToStore = uploadToStore;
            return this;
        }
        public ValueMonitorBuilder<T> function(Function<Long, T> function) {
            this.function = function;
            return this;
        }
        public ValueMonitorBuilder<T> supplier(Supplier<T> supplier) {
            this.function = ts -> supplier.get();
            return this;
        }
        public ValueMonitorBuilder<T> addTo(Collection<ValueMonitor<?>> addTo) {
            this.addTo = addTo;
            return this;
        }
        public ValueMonitor<T> build() {
            ValueMonitor<T> monitor = new ValueMonitorImpl<T>(id, name, value, uploadToStore);
            MonitoredValues.this.add(monitor);
            if (addTo != null) {
                addTo.add(monitor);
            }
            return monitor;
        }
        public PollableMonitor<T> buildPollable() {
            PollableMonitor<T> monitor = new PollableMonitorImpl<T>(id, name, function, uploadToStore);
            MonitoredValues.this.add(monitor);
            if (addTo != null) {
                addTo.add(monitor);
            }
            return monitor;
        }
        public AtomicIntegerMonitor buildAtomic() {
            int value = this.value == null ? 0 : (int)(Integer) this.value;
            AtomicIntegerMonitor monitor = new AtomicIntegerMonitor(id, name, value, uploadToStore);
            MonitoredValues.this.add(monitor);
            if (addTo != null) {
                addTo.add(monitor);
            }
            return monitor;
        }
    }

    @Override
    public void setAttribute(Attribute attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException {
        ValueMonitor<?> monitor = monitors.get(attribute);
        if (monitor == null) throw new AttributeNotFoundException();
        return monitor.getValue();
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        Translations translations;
        try {
            translations = Common.getTranslations();
        } catch (NullPointerException e) {
            translations = null;
        }
        Translations finalTranslations = translations;

        MBeanAttributeInfo[] attributes = monitors.values().stream().map(v -> new MBeanAttributeInfo(v.getId(), "",
                finalTranslations != null ? v.getName().translate(finalTranslations) : v.getId(),
                true, false, false)).toArray(MBeanAttributeInfo[]::new);
        return new MBeanInfo(getClass().getName(), "Internal monitored values", attributes,
                null, null, null);
    }
}
