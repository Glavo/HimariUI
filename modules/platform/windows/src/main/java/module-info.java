/// Defines the required Windows desktop platform implementation.
module org.glavo.himari.platform.windows {
    requires transitive org.glavo.himari.platform.api;
    requires transitive org.glavo.himari.layout;
    requires org.glavo.himari.ffi;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.platform.windows;
}
