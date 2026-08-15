package org.glavo.himari.rhi.d3d12;

import org.glavo.himari.graphics.Color;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.platform.windows.WindowsBackend;
import org.glavo.himari.platform.windows.WindowsPlatform;
import org.glavo.himari.platform.windows.WindowsWindow;
import org.glavo.himari.render.software.SoftwareSurface;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the production D3D12 device, format query, and SDR present path.
@NotNullByDefault
@EnabledOnOs(OS.WINDOWS)
final class D3d12DeviceTest {
    /// Creates a device and confirms the truthful SDR capability snapshot.
    @Test
    void queriesSdrRenderTargetSupport() {
        try (D3d12Device device = D3d12Device.open()) {
            D3d12Capabilities capabilities = device.capabilities();
            assertTrue(capabilities.r8g8b8a8RenderTarget());
            assertFalse(capabilities.hdrPresentationEnabled());
            assertEquals("color-managed-sdr", capabilities.presentationMode());
        }
    }

    /// Presents one flip-model SDR frame onto a production HWND.
    @Test
    void presentsSdrToWindowsWindow() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try (D3d12Device device = D3d12Device.open()) {
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI D3D12",
                            new LogicalRect(16.0, 16.0, 320.0, 240.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            D3d12Presentation presentation = device.presentSdr(window.nativeHandle(), 320, 240);
            assertTrue(presentation.presented());
            assertTrue(presentation.cleared());
            assertTrue(presentation.backBufferIndex() == 0 || presentation.backBufferIndex() == 1);
            assertEquals("DXGI_FORMAT_R8G8B8A8_UNORM", presentation.format());
            assertEquals("DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709", presentation.colorSpace());
            assertFalse(presentation.hdrMetadataApplied());
            assertTrue(presentation.ownedReferences() > 0);
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
    }

    /// Clears and presents two frames through a live swapchain.
    @Test
    void swapChainClearsTwoFrames() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try (D3d12Device device = D3d12Device.open()) {
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI D3D12 SwapChain",
                            new LogicalRect(16.0, 16.0, 320.0, 240.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            try (D3d12SwapChain swapChain = D3d12SwapChain.attach(device, window.nativeHandle(), 320, 240)) {
                D3d12Presentation first = swapChain.clearAndPresent(0.1f, 0.2f, 0.3f, 1.0f);
                D3d12Presentation second = swapChain.clearAndPresent(0.4f, 0.5f, 0.6f, 1.0f);
                assertTrue(first.cleared() && second.cleared());
                assertTrue(first.presented() && second.presented());
                assertTrue(second.ownedReferences() >= first.ownedReferences());
            }
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
    }

    /// Writes an upload buffer through `Map` and reads the same bytes back.
    @Test
    void uploadBufferRoundTripsMappedBytes() {
        try (D3d12Device device = D3d12Device.open();
             D3d12GpuResource resource = device.createUploadResource(new byte[] { 0x11, 0x22, 0x33, 0x44 })) {
            assertTrue(resource.descriptorIncrement() > 0);
            assertTrue(resource.ownedReferences() >= 2);
            assertEquals(4, resource.payload().length);
            assertEquals(0x11, resource.readBack(device)[0] & 0xFF);
            assertEquals(0x44, resource.readBack(device)[3] & 0xFF);
        }
    }

    /// Uploads software-raster RGBA through a 2D texture and reads the same bytes back.
    @Test
    void textureRoundTripMatchesSoftwareRgba() {
        SoftwareSurface surface = new SoftwareSurface(16, 8);
        surface.clear(Color.srgb(0.2f, 0.4f, 0.8f, 1.0f));
        byte[] expected = surface.toSdrRgba();
        try (D3d12Device device = D3d12Device.open()) {
            D3d12TextureRoundTrip trip = device.roundTripSdrRgba(expected, 16, 8);
            assertTrue(trip.copied());
            assertEquals(0, trip.maxChannelDelta(expected));
            assertEquals(expected.length, trip.matchedBytes(expected));
        }
    }

    /// Clears an offscreen render target and stays within one 8-bit channel of the software clear.
    @Test
    void gpuClearDiffersFromSoftwareByAtMostOneChannel() {
        SoftwareSurface surface = new SoftwareSurface(16, 8);
        surface.clear(Color.srgb(0.2f, 0.4f, 0.8f, 1.0f));
        byte[] expected = surface.toSdrRgba();
        try (D3d12Device device = D3d12Device.open()) {
            D3d12TextureRoundTrip trip = device.clearSdrAndReadback(0.2f, 0.4f, 0.8f, 1.0f, 16, 8);
            assertTrue(trip.copied());
            assertTrue(trip.maxChannelDelta(expected) <= 1);
        }
    }

    /// Presents a software RGBA frame onto a production HWND through D3D12.
    @Test
    void presentsSoftwareRgbaToWindowsWindow() throws Exception {
        SoftwareSurface surface = new SoftwareSurface(32, 16);
        surface.clear(Color.srgb(0.1f, 0.5f, 0.3f, 1.0f));
        byte[] rgba = surface.toSdrRgba();
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try (D3d12Device device = D3d12Device.open()) {
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI D3D12 Texture",
                            new LogicalRect(16.0, 16.0, 320.0, 240.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            D3d12Presentation presentation = device.presentSdrRgba(window.nativeHandle(), rgba, 32, 16);
            assertTrue(presentation.presented());
            assertFalse(presentation.cleared());
            assertFalse(presentation.hdrMetadataApplied());
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
    }

    /// Copies bytes through a default-heap buffer and reads them back.
    @Test
    void copiesUploadThroughDefaultHeap() {
        byte[] payload = { 0x10, 0x20, 0x30, 0x40, 0x50 };
        try (D3d12Device device = D3d12Device.open()) {
            byte[] copied = device.copyThroughDefaultHeap(payload);
            assertEquals(payload.length, copied.length);
            for (int index = 0; index < payload.length; index++) {
                assertEquals(payload[index], copied[index]);
            }
        }
    }
}
