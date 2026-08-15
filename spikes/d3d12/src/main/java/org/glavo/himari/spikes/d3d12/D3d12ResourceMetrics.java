package org.glavo.himari.spikes.d3d12;

import org.jetbrains.annotations.NotNullByDefault;

/// Captures process-resource observations surrounding one D3D12 scenario.
///
/// @param processHandlesBefore the baseline process handle count
/// @param processHandlesAfter the post-teardown process handle count
/// @param jvmLiveThreadsBefore the baseline live-thread count
/// @param jvmLiveThreadsAfter the post-teardown live-thread count
/// @param heapUsedBytesBefore the baseline used Java heap
/// @param heapUsedBytesAfter the post-teardown used Java heap
/// @param processPrivateBytesBefore the baseline process private committed bytes
/// @param processPrivateBytesAfter the post-teardown process private committed bytes
/// @param processPeakCommittedBytesAfter the post-teardown process peak committed bytes
@NotNullByDefault
record D3d12ResourceMetrics(
        int processHandlesBefore,
        int processHandlesAfter,
        int jvmLiveThreadsBefore,
        int jvmLiveThreadsAfter,
        long heapUsedBytesBefore,
        long heapUsedBytesAfter,
        long processPrivateBytesBefore,
        long processPrivateBytesAfter,
        long processPeakCommittedBytesAfter
) {
}
