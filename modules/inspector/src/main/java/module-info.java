/// Defines the first-stable tree, semantics, and runtime-trace inspector.
module org.glavo.himari.inspector {
    requires transitive org.glavo.himari.layout;
    requires transitive org.glavo.himari.runtime;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.inspector;
}
