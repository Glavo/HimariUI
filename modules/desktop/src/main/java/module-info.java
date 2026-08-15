/// Defines the first-stable HimariUI desktop application entry.
module org.glavo.himari.desktop {
    requires org.glavo.himari.controls;
    requires org.glavo.himari.font;
    requires org.glavo.himari.graphics;
    requires org.glavo.himari.inspector;
    requires org.glavo.himari.layout;
    requires org.glavo.himari.objc;
    requires org.glavo.himari.platform.api;
    requires org.glavo.himari.platform.headless;
    requires org.glavo.himari.platform.macos;
    requires org.glavo.himari.platform.wayland;
    requires org.glavo.himari.platform.windows;
    requires org.glavo.himari.render.software;
    requires org.glavo.himari.rhi.d3d12;
    requires org.glavo.himari.rhi.metal;
    requires org.glavo.himari.runtime;
    requires org.glavo.himari.state;
    requires org.glavo.himari.text;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.desktop;
}
