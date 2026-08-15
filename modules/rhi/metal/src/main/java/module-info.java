/// Defines the production Metal RHI backend implemented through generated FFM bindings.
@SuppressWarnings("module")
module org.glavo.himari.rhi.metal {
    requires org.glavo.himari.ffi;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.rhi.metal;
}
