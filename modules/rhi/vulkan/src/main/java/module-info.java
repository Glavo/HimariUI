/// Defines the production Vulkan RHI backend implemented through generated FFM bindings.
@SuppressWarnings("module")
module org.glavo.himari.rhi.vulkan {
    requires org.glavo.himari.ffi;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.rhi.vulkan;
}
