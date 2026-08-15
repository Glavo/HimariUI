/// Defines the HimariUI layout, input, focus, semantics, and hit-testing module.
module org.glavo.himari.layout {
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.layout;
    exports org.glavo.himari.layout.bootstrap;
    exports org.glavo.himari.layout.focus;
    exports org.glavo.himari.layout.hit;
    exports org.glavo.himari.layout.input;
    exports org.glavo.himari.layout.semantics;
}
