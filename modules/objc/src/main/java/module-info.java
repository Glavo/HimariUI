/// Defines the production Objective-C block ABI implemented through generated FFM bindings.
@SuppressWarnings("module")
module org.glavo.himari.objc {
    requires org.glavo.himari.ffi;
    requires static transitive org.jetbrains.annotations;

    exports org.glavo.himari.objc;
}
