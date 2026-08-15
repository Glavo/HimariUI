package org.glavo.himari.spikes.runtime.hybrid;

import org.glavo.himari.spikes.runtime.sample.ComparisonEnvironment;
import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.glavo.himari.spikes.runtime.sample.FixtureCommand;
import org.glavo.himari.spikes.runtime.sample.RuntimePhase;
import org.glavo.himari.state.BooleanState;
import org.glavo.himari.state.MutableState;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Implements the realistic settings form and keyed chat list in ordinary Java.
///
/// Persistent owners initialize settings, chat, and advanced-panel nodes once. Fine-grained
/// bindings update stable properties, while two small structural scopes are the only operations
/// permitted to change topology.
@NotNullByDefault
final class HybridSettingsChatApplication extends HybridFixtureSession {
    /// The selected settings theme.
    private final MutableState<String> theme;

    /// Whether notifications are enabled.
    private final BooleanState notifications;

    /// The editor-owned draft text.
    private final MutableState<String> draft;

    /// The selected message filter.
    private final MutableState<String> filter;

    /// The visible message-key source consumed by the explicit collection controller.
    private final MutableState<@Unmodifiable List<String>> visibleKeys;

    /// All chat messages in insertion order.
    private final ArrayList<Message> messages = new ArrayList<>();

    /// Whether advanced settings are visible.
    private final BooleanState advancedVisible;

    /// The theme property last written by its binding.
    private String renderedTheme = "";

    /// The notification property last written by its binding.
    private boolean renderedNotifications;

    /// The draft property last written by its binding.
    private String renderedDraft = "";

    /// The filter property last written by its binding.
    private String renderedFilter = "";

    /// The message collection scope, or `null` before mount.
    private @Nullable HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> reactions;

    /// The retained advanced-panel scope, or `null` before mount.
    private @Nullable HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> advanced;

    /// A new message awaiting its mount and post-commit effects, or `null`.
    private @Nullable String pendingSentKey;

    /// Creates the realistic application model.
    ///
    /// @param environment the fresh environment
    /// @param probe the shared instrumentation probe
    HybridSettingsChatApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
        super(environment, probe);
        theme = domain.mutableState("system");
        notifications = domain.booleanState(true);
        draft = domain.mutableState("");
        filter = domain.mutableState("all");
        messages.add(new Message("m1", false));
        messages.add(new Message("m2", true));
        messages.add(new Message("m3", false));
        visibleKeys = domain.mutableState(List.of("m1", "m2", "m3"));
        advancedVisible = domain.booleanState(false);
    }

    /// Mounts settings, chat, messages, and the retained advanced-panel anchor once.
    ///
    /// @param root the root owner
    @Override
    protected void initialize(HybridRuntime.Owner root) {
        root.node("root");
        initializeSettings(root);
        initializeChat(root);
        initializeAdvancedSettings(root);
    }

    /// Applies one settings, editor, chat, reaction, filter, or advanced-panel command.
    ///
    /// @param command the command
    /// @return whether reactive sources must be flushed
    @Override
    protected boolean handle(FixtureCommand command) {
        return switch (command.operation()) {
            case "set-preferences" -> {
                setPreferences(argument(command, "value"));
                yield true;
            }
            case "set-draft" -> {
                draft.set(argument(command, "value"));
                yield true;
            }
            case "send-draft" -> {
                sendDraft();
                yield true;
            }
            case "increment-reaction" -> {
                reaction(argument(command, "key")).increment();
                yield false;
            }
            case "set-filter" -> {
                setFilter(argument(command, "value"));
                yield true;
            }
            case "show-advanced" -> {
                advancedVisible.set(true);
                yield true;
            }
            case "increment-advanced" -> {
                advanced().increment();
                yield false;
            }
            default -> throw unknown(command);
        };
    }

    /// Returns realistic fixture values from bound properties and owner-local cells.
    ///
    /// @return the immutable values
    @Override
    protected @Unmodifiable Map<String, String> values() {
        @Nullable HybridRuntime.LocalInt advancedLocal = advancedController().value("panel");
        return valuesOf(
                "advanced.local", advancedLocal == null ? "absent" : Integer.toString(advancedLocal.get()),
                "advanced.visible", Boolean.toString(advancedVisible.get()),
                "chat.draft", renderedDraft,
                "chat.filter", renderedFilter,
                "chat.messages", String.join(",", reactionController().keys()),
                "message.m2.reactions", Integer.toString(reaction("m2").get()),
                "settings.notifications", Boolean.toString(renderedNotifications),
                "settings.theme", renderedTheme
        );
    }

    /// Initializes settings controls and their property bindings.
    ///
    /// @param root the root owner
    private void initializeSettings(HybridRuntime.Owner root) {
        root.component("settings-owner", settings -> {
            settings.node("settings");
            settings.bind(
                    theme,
                    "theme-field",
                    () -> renderedTheme = theme.get(),
                    RuntimePhase.PAINT,
                    RuntimePhase.SEMANTICS
            );
            settings.node("theme-field");
            settings.bind(
                    notifications,
                    "notification-toggle",
                    () -> renderedNotifications = notifications.get(),
                    RuntimePhase.PAINT,
                    RuntimePhase.SEMANTICS
            );
            settings.node("notification-toggle");
        });
    }

    /// Initializes draft editing and the visible keyed message controller.
    ///
    /// @param root the root owner
    private void initializeChat(HybridRuntime.Owner root) {
        root.component("chat-owner", chat -> {
            chat.node("chat");
            chat.bind(
                    draft,
                    "draft-editor",
                    () -> renderedDraft = draft.get(),
                    RuntimePhase.MEASURE,
                    RuntimePhase.PAINT,
                    RuntimePhase.SEMANTICS
            );
            chat.node("draft-editor");
            chat.bind(filter, "message-filter", () -> renderedFilter = filter.get(), RuntimePhase.STRUCTURE);
            reactions = chat.structure(
                    "message-rows",
                    visibleKeys,
                    scope -> {
                        for (String key : visibleKeys.get()) {
                            scope.fragment(key, HybridRuntime.Retention.DISPOSE, message -> {
                                HybridRuntime.LocalInt local = message.localInt(0);
                                message.node("message:" + key);
                                message.effect(
                                        () -> {
                                            if (key.equals(pendingSentKey)) {
                                                emit("mount-message:" + key);
                                            }
                                        },
                                        () -> {
                                        }
                                );
                                message.onCommit(() -> {
                                    if (key.equals(pendingSentKey)) {
                                        emit("effect:send:" + key);
                                        pendingSentKey = null;
                                    }
                                });
                                return local;
                            });
                        }
                    },
                    RuntimePhase.STRUCTURE
            );
        });
    }

    /// Initializes the retain-on-hide advanced settings panel.
    ///
    /// @param root the root owner
    private void initializeAdvancedSettings(HybridRuntime.Owner root) {
        advanced = root.structure(
                "advanced-settings",
                advancedVisible,
                scope -> {
                    if (advancedVisible.get()) {
                        scope.fragment("panel", HybridRuntime.Retention.RETAIN, panel -> {
                            HybridRuntime.LocalInt local = panel.localInt(0);
                            panel.node("advanced-settings");
                            panel.effect(
                                    () -> emit("effect-mount:advanced"),
                                    () -> emit("effect-dispose:advanced")
                            );
                            return local;
                        });
                    }
                },
                RuntimePhase.STRUCTURE
        );
    }

    /// Atomically replaces the two settings encoded as `theme:notifications`.
    ///
    /// @param encoded the frozen settings encoding
    private void setPreferences(String encoded) {
        String[] values = encoded.split(":", -1);
        if (values.length != 2) {
            throw new IllegalArgumentException("Preferences require theme:notifications");
        }
        transaction(() -> {
            theme.set(values[0]);
            notifications.set(Boolean.parseBoolean(values[1]));
        });
        emit("preferences-commit");
    }

    /// Appends one unread message from the current nonempty draft and clears the editor.
    private void sendDraft() {
        if (draft.get().isEmpty()) {
            throw new IllegalStateException("Cannot send an empty draft");
        }
        String key = "m" + (messages.size() + 1);
        messages.add(new Message(key, true));
        pendingSentKey = key;
        transaction(() -> {
            draft.set("");
            visibleKeys.set(computeVisibleKeys(filter.get()));
        });
    }

    /// Replaces the filter and its derived structural key source in one publication.
    ///
    /// @param value the new filter
    private void setFilter(String value) {
        if (!value.equals("all") && !value.equals("unread")) {
            throw new IllegalArgumentException("Unknown message filter: " + value);
        }
        transaction(() -> {
            filter.set(value);
            visibleKeys.set(computeVisibleKeys(value));
        });
    }

    /// Computes visible message keys for one validated filter value.
    ///
    /// @param selectedFilter the selected filter
    /// @return the immutable visible keys
    private @Unmodifiable List<String> computeVisibleKeys(String selectedFilter) {
        ArrayList<String> keys = new ArrayList<>();
        for (Message message : messages) {
            if (selectedFilter.equals("all") || message.unread()) {
                keys.add(message.key());
            }
        }
        return List.copyOf(keys);
    }

    /// Returns the mounted message controller.
    ///
    /// @return the controller
    private HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> reactionController() {
        if (reactions == null) {
            throw new IllegalStateException("Message controller is unavailable");
        }
        return reactions;
    }

    /// Returns one required visible message reaction cell.
    ///
    /// @param key the message key
    /// @return the reaction cell
    private HybridRuntime.LocalInt reaction(String key) {
        @Nullable HybridRuntime.LocalInt local = reactionController().value(key);
        if (local == null) {
            throw new IllegalStateException("Message has no committed reaction state: " + key);
        }
        return local;
    }

    /// Returns the mounted advanced-panel controller.
    ///
    /// @return the controller
    private HybridRuntime.StructuralScope<String, HybridRuntime.LocalInt> advancedController() {
        if (advanced == null) {
            throw new IllegalStateException("Advanced controller is unavailable");
        }
        return advanced;
    }

    /// Returns the visible advanced-panel local cell.
    ///
    /// @return the local cell
    private HybridRuntime.LocalInt advanced() {
        @Nullable HybridRuntime.LocalInt local = advancedController().value("panel");
        if (local == null || !advancedController().visible("panel")) {
            throw new IllegalStateException("Advanced settings are not visible");
        }
        return local;
    }

    /// Stores immutable message identity and unread state.
    ///
    /// @param key the semantic key
    /// @param unread whether the unread filter includes the message
    @NotNullByDefault
    private record Message(String key, boolean unread) {
        /// Creates a validated message.
        private Message {
            if (key.isBlank()) {
                throw new IllegalArgumentException("Message key must not be blank");
            }
        }
    }
}

