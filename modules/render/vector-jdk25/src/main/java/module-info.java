/// Defines the optional HimariUI vector fill accelerator.
///
/// The public API uses ordinary float arrays so the module can be removed without
/// changing `himari-render-software` or exposing incubator Vector types.
module org.glavo.himari.render.vector {
    requires transitive org.glavo.himari.graphics;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.render.vector;
}
