package org.glavo.himari.spikes.d3d12;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/// Captures the complete observation produced by one D3D12 device and swapchain lifecycle.
///
/// @param repetitions the requested presentation count
/// @param requestedSoakSeconds the requested minimum run duration
/// @param elapsedMillis the observed run duration
/// @param width the swapchain width in physical pixels
/// @param height the swapchain height in physical pixels
/// @param rowPitch the readback row pitch in bytes
/// @param presentedFrames the number of successful `Present` calls
/// @param readbackVerifiedFrames the number of frames whose complete readback was verified
/// @param verifiedPixelCount the total number of verified pixels
/// @param maximumChannelDelta the greatest byte delta from the deterministic fixture
/// @param finalFenceValue the final signalled and completed fence value
/// @param deviceRemovedReason the final `ID3D12Device::GetDeviceRemovedReason` result
/// @param debugLayerEnabled whether `ID3D12Debug::EnableDebugLayer` ran
/// @param dxgiFactoryDebugEnabled whether the DXGI factory accepted its debug flag
/// @param infoQueueAvailable whether `ID3D12InfoQueue` was acquired
/// @param formats the immutable format-support observations
/// @param colorSpaces the immutable color-space observations for the selected SDR swapchain
/// @param debugMessages the immutable debug-queue messages copied before teardown
/// @param ownedComReferences the number of acquired COM references
/// @param releasedComReferences the number of released COM references
/// @param declaredResourceBytes the byte extent declared for the swapchain and readback resources
@NotNullByDefault
record D3d12ScenarioResult(
        int repetitions,
        int requestedSoakSeconds,
        long elapsedMillis,
        int width,
        int height,
        int rowPitch,
        int presentedFrames,
        int readbackVerifiedFrames,
        long verifiedPixelCount,
        int maximumChannelDelta,
        long finalFenceValue,
        int deviceRemovedReason,
        boolean debugLayerEnabled,
        boolean dxgiFactoryDebugEnabled,
        boolean infoQueueAvailable,
        @Unmodifiable List<D3d12FormatSupport> formats,
        @Unmodifiable List<D3d12ColorSpaceSupport> colorSpaces,
        @Unmodifiable List<D3d12DebugMessage> debugMessages,
        int ownedComReferences,
        int releasedComReferences,
        long declaredResourceBytes
) {
    /// Copies every collection to detach the observation from mutable builders.
    D3d12ScenarioResult {
        formats = List.copyOf(formats);
        colorSpaces = List.copyOf(colorSpaces);
        debugMessages = List.copyOf(debugMessages);
    }

    /// Returns the number of debug-layer errors and corruption reports.
    ///
    /// @return the error count
    long debugErrorCount() {
        return debugMessages.stream().filter(D3d12DebugMessage::isError).count();
    }

    /// Returns the number of debug-layer warnings.
    ///
    /// @return the warning count
    long debugWarningCount() {
        return debugMessages.stream().filter(message -> message.severity() == 2).count();
    }
}
