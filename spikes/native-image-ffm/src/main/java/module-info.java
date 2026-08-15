/// Composes the portable FFM and Windows D3D12 spikes inside one Native Image executable.
module org.glavo.himari.spikes.nativeimage.ffm {
    requires org.glavo.himari.ffi;
    requires org.glavo.himari.spikes.d3d12;
    requires org.glavo.himari.spikes.ffi.ffm;
    requires static org.jetbrains.annotations;
}
