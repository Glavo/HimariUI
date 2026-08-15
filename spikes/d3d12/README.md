# D3D12 surface feasibility spike

This module implements `SPIKE-D3D12-001`. On Windows x64 it uses generated Java 25 FFM bindings to create a D3D12 device, direct command queue, DXGI flip-model swapchain, render-target descriptors, command allocator/list, fence, and readback resource. It presents and reads back a deterministic SDR clear, records format and color-space support, inspects the D3D12 debug message queue when available, and releases every owned COM reference deterministically.

The schema describes the target-resolved COM ABI, not only the apparent C++ signatures. In particular, the Windows C vtable lowers `ID3D12DescriptorHeap::GetCPUDescriptorHandleForHeapStart` to a returned pointer plus an explicit structure output parameter. Keeping that lowering in the fixture avoids relying on a generic aggregate-return convention that is incompatible with the actual Windows interface.

Run the short smoke profile with:

```text
./gradlew -g .gradle-user-home :spikes:d3d12:d3d12Smoke
```

Run the complete 300-second M0 profile with:

```text
./gradlew -g .gradle-user-home :spikes:d3d12:conformance
```

The canonical ABI fixture currently targets Windows x64. Windows arm64 remains a separate required conformance environment and must receive a target-specific generated fixture before the profile can pass globally.
