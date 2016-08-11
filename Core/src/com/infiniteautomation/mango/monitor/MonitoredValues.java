package com.infiniteautomation.mango.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matthew Lohbihler
 */
public class MonitoredValues {
    // Monitored values.
    private final List<ValueMonitor<?>> monitors = new ArrayList<ValueMonitor<?>>();

    @SuppressWarnings("unchecked")
    public <T> ValueMonitor<T> addIfMissingStatMonitor(ValueMonitor<T> monitor) {
        ValueMonitor<?> m = getValueMonitor(monitor.getId());
        boolean found = false;
        if (m != null) {
            monitor = (ValueMonitor<T>) m;
            found = true;
        }

        if (found)
            // Remove it so that it can be added back in in the desired order
            monitors.remove(monitor);

        // Add it.
        monitors.add(monitor);

        return monitor;
    }

    //    public void removeStatMonitor(String id) {
    //        for (ValueMonitor<?> m : monitors) {
    //            if (m.getId().equals(id)) {
    //                monitors.remove(m);
    //                break;
    //            }
    //        }
    //    }

    public List<ValueMonitor<?>> getMonitors() {
        return monitors;
    }

    public ValueMonitor<?> getValueMonitor(String id) {
        for (ValueMonitor<?> m : monitors) {
            if (m.getId().equals(id))
                return m;
        }
        return null;
    }
}
