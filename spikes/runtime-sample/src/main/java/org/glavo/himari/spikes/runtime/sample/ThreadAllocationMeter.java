package org.glavo.himari.spikes.runtime.sample;

import com.sun.management.ThreadMXBean;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;

/// Measures exact bytes allocated by the current JVM thread when the JDK counter is available.
@NotNullByDefault
final class ThreadAllocationMeter {
    /// The enabled allocation bean, or `null` when the runtime does not provide the counter.
    private final @Nullable ThreadMXBean bean;

    /// Creates a meter and enables thread-allocation measurement when supported.
    ThreadAllocationMeter() {
        @Nullable ThreadMXBean candidate = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
        if (candidate == null || !candidate.isThreadAllocatedMemorySupported()) {
            bean = null;
            return;
        }
        if (!candidate.isThreadAllocatedMemoryEnabled()) {
            candidate.setThreadAllocatedMemoryEnabled(true);
        }
        bean = candidate.isThreadAllocatedMemoryEnabled() ? candidate : null;
    }

    /// Captures a window start value.
    ///
    /// @return the current byte counter, or zero when unavailable
    long start() {
        return bean == null ? 0L : Math.max(0L, bean.getCurrentThreadAllocatedBytes());
    }

    /// Completes a measurement window.
    ///
    /// @param start the value returned by [#start()]
    /// @return the allocation measurement
    AllocationMeasurement finish(long start) {
        if (bean == null) {
            return new AllocationMeasurement(false, 0L);
        }
        long end = bean.getCurrentThreadAllocatedBytes();
        if (end < start) {
            throw new IllegalStateException("Current-thread allocation counter moved backwards");
        }
        return new AllocationMeasurement(true, end - start);
    }
}
