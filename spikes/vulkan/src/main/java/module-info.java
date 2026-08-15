/// Probes the production Vulkan backend for M0 feasibility.
module org.glavo.himari.spikes.vulkan {
    requires org.glavo.himari.rhi.vulkan;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.spikes.vulkan;
}
