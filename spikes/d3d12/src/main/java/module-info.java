/// Exercises generated D3D12, DXGI, and Kernel32 bindings for M0 feasibility evidence.
@SuppressWarnings("module")
module org.glavo.himari.spikes.d3d12 {
    exports org.glavo.himari.spikes.d3d12;

    requires java.management;
    requires org.glavo.himari.ffi;
    requires org.glavo.himari.spikes.windows;
    requires static transitive org.jetbrains.annotations;
}
