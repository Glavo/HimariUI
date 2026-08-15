package org.glavo.himari.rhi.d3d12;

import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.platform.windows.WindowsBackend;
import org.glavo.himari.platform.windows.WindowsPlatform;
import org.glavo.himari.platform.windows.WindowsWindow;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes production D3D12 device and SDR present evidence.
@NotNullByDefault
public final class D3d12Conformance {
    /// Prevents instantiation.
    private D3d12Conformance() {
    }

    /// Opens a device, presents once to a production HWND, and writes artifacts.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        D3d12Capabilities capabilities;
        D3d12Presentation presentation;
        int descriptorIncrement = 0;
        int resourceReferences = 0;
        boolean gpuCopy = false;
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try (D3d12Device device = D3d12Device.open()) {
            capabilities = device.capabilities();
            if (!capabilities.r8g8b8a8RenderTarget()) {
                throw new IllegalStateException("R8G8B8A8 is not a render target");
            }
            if (capabilities.hdrPresentationEnabled()) {
                throw new IllegalStateException("D3D12 backend claimed HDR presentation");
            }
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI D3D12 Conformance",
                            new LogicalRect(16.0, 16.0, 320.0, 240.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            presentation = device.presentSdr(window.nativeHandle(), 320, 240);
            if (!presentation.cleared() || !presentation.presented()) {
                throw new IllegalStateException("D3D12 swapchain did not clear and present");
            }
            try (D3d12GpuResource resource = device.createUploadResource(new byte[] { 7, 8, 9, 10 })) {
                byte[] readBack = resource.readBack(device);
                if (readBack.length != 4 || readBack[0] != 7 || readBack[3] != 10) {
                    throw new IllegalStateException("D3D12 upload buffer did not round-trip mapped bytes");
                }
                descriptorIncrement = resource.descriptorIncrement();
                resourceReferences = resource.ownedReferences();
            }
            byte[] copied = device.copyThroughDefaultHeap(new byte[] { 1, 2, 3, 4 });
            gpuCopy = copied.length == 4 && copied[0] == 1 && copied[3] == 4;
            if (!gpuCopy) {
                throw new IllegalStateException("D3D12 default-heap copy did not round-trip");
            }
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("capabilities.json"),
                """
                        {
                          "profile": "m6-d3d12",
                          "r8g8b8a8RenderTarget": %s,
                          "support1": %d,
                          "hdrPresentationEnabled": false,
                          "presentationMode": "%s"
                        }
                        """.formatted(
                        capabilities.r8g8b8a8RenderTarget(),
                        capabilities.support1(),
                        capabilities.presentationMode()
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("presentation.json"),
                """
                        {
                          "presented": %s,
                          "cleared": %s,
                          "backBufferIndex": %d,
                          "format": "%s",
                          "colorSpace": "%s",
                          "hdrMetadataApplied": false,
                          "ownedReferences": %d,
                          "releasedReferences": %d
                        }
                        """.formatted(
                        presentation.presented(),
                        presentation.cleared(),
                        presentation.backBufferIndex(),
                        presentation.format(),
                        presentation.colorSpace(),
                        presentation.ownedReferences(),
                        presentation.releasedReferences()
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m6-d3d12",
                          "workPackage": "D3D12-001",
                          "status": "passed",
                          "deviceCreated": true,
                          "sdrPresent": true,
                          "cleared": true,
                          "committedResource": true,
                          "descriptorIncrement": %d,
                          "resourceReferences": %d,
                          "gpuCopy": %s,
                          "hdrClaimed": false
                        }
                        """.formatted(descriptorIncrement, resourceReferences, gpuCopy),
                StandardCharsets.UTF_8
        );
    }
}
