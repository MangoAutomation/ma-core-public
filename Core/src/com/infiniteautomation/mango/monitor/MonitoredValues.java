package com.infiniteautomation.mango.monitor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * List of Values Monitored for Mango
 * 
 * @author Matthew Lohbihler, Terry Packer
 */
public class MonitoredValues {
	
	// Monitored values.
	private final List<ValueMonitor<?>> monitors = new CopyOnWriteArrayList<ValueMonitor<?>>();

	/**
	 * Add Monitor if it is missing, if already exists it will be re-added at the end of the list.
	 *
	 * @param monitor
	 * @return provided monitor if it wasn't already in the list else the monitor that was in the list
	 */
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

	/**
	 * Remove monitor with given id
	 * @param id
	 */
	public void removeStatMonitor(String id) {
		for (ValueMonitor<?> m : monitors) {
			if (m.getId().equals(id)) {
				monitors.remove(m);
				break;
			}
		}
	}

	/**
	 * List all Monitors
	 * @return
	 */
	public List<ValueMonitor<?>> getMonitors() {
		return monitors;
	}

	/**
	 * Get the monitor with given Id
	 * @param id
	 * @return
	 */
	public ValueMonitor<?> getValueMonitor(String id) {
		for (ValueMonitor<?> m : monitors) {
			if (m.getId().equals(id))
				return m;
		}
		return null;
	}
	
	/**
	 * Force reset of all Monitored values to some known state/value.
	 * Useful for counters that can get out of sync with their external source.
	 */
	public void reset(){
		for (ValueMonitor<?> m : monitors) {
			m.reset();
		}
	}
	
}
