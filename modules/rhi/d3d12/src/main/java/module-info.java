/// Defines the production D3D12 RHI backend implemented through generated FFM bindings.
@SuppressWarnings("module")
module org.glavo.himari.rhi.d3d12 {
    requires org.glavo.himari.ffi;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.rhi.d3d12;
}
