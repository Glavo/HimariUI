/// Defines the production macOS platform backend implemented through generated FFM bindings.
@SuppressWarnings("module")
module org.glavo.himari.platform.macos {
    requires transitive org.glavo.himari.platform.api;
    requires org.glavo.himari.ffi;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.platform.macos;
}
