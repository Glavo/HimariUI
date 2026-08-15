package org.glavo.himari.spikes.runtime.grouped;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Implements the realistic settings form and keyed chat list in ordinary Java.
///
/// The application intentionally uses only the same explicit grouped API available to the micro
/// and integration ports. Settings use phase bindings, messages use semantic-keyed reconciliation,
/// and the advanced panel declares retain-on-hide memory and a scoped effect.
@NotNullByDefault
final class GroupedSettingsChatApplication extends GroupedFixtureSession {
    /// The selected settings theme.
    private final MutableState<String> theme;

    /// Whether notifications are enabled.
    private final BooleanState notifications;

    /// The editor-owned draft text.
    private final MutableState<String> draft;

    /// The selected message filter.
    private final MutableState<String> filter;

    /// All application-owned chat messages in insertion order.
    private final ArrayList<Message> messages = new ArrayList<>();

    /// Whether advanced settings are visible.
    private final BooleanState advancedVisible;

    /// Committed reaction locals for visible keyed messages.
    private @Unmodifiable Map<String, GroupedRuntime.LocalInt> reactionLocals = Map.of();

    /// The retained advanced-settings local, or `null` before first visibility.
    private @Nullable GroupedRuntime.LocalInt advancedLocal;

    /// A newly sent message awaiting its post-mount send effect, or `null`.
    private @Nullable String pendingSentKey;

    /// Creates the realistic application model.
    ///
    /// @param environment the fresh environment
    /// @param probe the shared instrumentation probe
    GroupedSettingsChatApplication(ComparisonEnvironment environment, ComparisonProbe probe) {
        super(environment, probe);
        theme = domain.mutableState("system");
        notifications = domain.booleanState(true);
        draft = domain.mutableState("");
        filter = domain.mutableState("all");
        advancedVisible = domain.booleanState(false);
        messages.add(new Message("m1", false));
        messages.add(new Message("m2", true));
        messages.add(new Message("m3", false));
    }

    /// Declares the settings form, editor, keyed messages, and retained advanced branch.
    ///
    /// @param scope the explicit grouped scope
    @Override
    protected void compose(GroupedRuntime.Scope scope) {
        scope.node("root");
        declareSettings(scope);
        declareChat(scope);
        declareAdvancedSettings(scope);
        @Nullable String sentKey = pendingSentKey;
        if (sentKey != null) {
            scope.onCommit(() -> {
                emit("effect:send:" + sentKey);
                pendingSentKey = null;
            });
        }
    }

    /// Applies one settings, editor, chat, reaction, filter, or advanced-panel command.
    ///
    /// @param command the command
    /// @return always `true`
    @Override
    protected boolean handle(FixtureCommand command) {
        switch (command.operation()) {
            case "set-preferences" -> setPreferences(argument(command, "value"));
            case "set-draft" -> draft.set(argument(command, "value"));
            case "send-draft" -> sendDraft();
            case "increment-reaction" -> reaction(argument(command, "key")).increment();
            case "set-filter" -> filter.set(argument(command, "value"));
            case "show-advanced" -> advancedVisible.set(true);
            case "increment-advanced" -> advanced().increment();
            default -> throw unknown(command);
        }
        return true;
    }

    /// Returns all realistic fixture values from application state and committed positional memory.
    ///
    /// @return the immutable values
    @Override
    protected @Unmodifiable Map<String, String> values() {
        List<Message> visible = visibleMessages();
        return valuesOf(
                "advanced.local", advancedLocal == null ? "absent" : Integer.toString(advancedLocal.get()),
                "advanced.visible", Boolean.toString(advancedVisible.get()),
                "chat.draft", draft.get(),
                "chat.filter", filter.get(),
                "chat.messages", messageKeys(visible),
                "message.m2.reactions", Integer.toString(reaction("m2").get()),
                "settings.notifications", Boolean.toString(notifications.get()),
                "settings.theme", theme.get()
        );
    }

    /// Declares settings controls and their actual phase read sites.
    ///
    /// @param scope the grouped scope
    private void declareSettings(GroupedRuntime.Scope scope) {
        scope.group("settings", () -> {
            scope.node("settings");
            scope.binding(theme, "theme-field", RuntimePhase.PAINT, RuntimePhase.SEMANTICS);
            scope.node("theme-field");
            scope.binding(notifications, "notification-toggle", RuntimePhase.PAINT, RuntimePhase.SEMANTICS);
            scope.node("notification-toggle");
        });
    }

    /// Declares draft editing and the visible keyed message collection.
    ///
    /// @param scope the grouped scope
    private void declareChat(GroupedRuntime.Scope scope) {
        scope.group("chat", () -> {
            scope.node("chat");
            scope.binding(draft, "draft-editor", RuntimePhase.MEASURE, RuntimePhase.PAINT, RuntimePhase.SEMANTICS);
            scope.node("draft-editor");
            scope.binding(filter, "message-filter", RuntimePhase.STRUCTURE);
            List<Message> visible = visibleMessages();
            LinkedHashMap<String, GroupedRuntime.LocalInt> nextReactions = new LinkedHashMap<>();
            for (Message message : visible) {
                String key = message.key();
                scope.keyedGroup("message-row", key, () -> {
                    GroupedRuntime.LocalInt reactions = scope.rememberInt(0);
                    nextReactions.put(key, reactions);
                    scope.node("message:" + key);
                    scope.effect(
                            "message-lifetime",
                            () -> {
                                if (key.equals(pendingSentKey)) {
                                    emit("mount-message:" + key);
                                }
                            },
                            () -> {
                            }
                    );
                });
            }
            scope.onCommit(() -> reactionLocals = Map.copyOf(nextReactions));
        });
    }

    /// Declares the retain-on-hide advanced settings panel.
    ///
    /// @param scope the grouped scope
    private void declareAdvancedSettings(GroupedRuntime.Scope scope) {
        scope.binding(advancedVisible, "advanced-visible", RuntimePhase.STRUCTURE);
        scope.branch("advanced-settings", advancedVisible.get(), true, () -> {
            GroupedRuntime.LocalInt local = scope.rememberInt(0);
            scope.onCommit(() -> advancedLocal = local);
            scope.node("advanced-settings");
            scope.effect(
                    "advanced-lifetime",
                    () -> emit("effect-mount:advanced"),
                    () -> emit("effect-dispose:advanced")
            );
        });
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
        draft.set("");
        pendingSentKey = key;
    }

    /// Returns messages accepted by the current filter in insertion order.
    ///
    /// @return the immutable visible list
    private @Unmodifiable List<Message> visibleMessages() {
        if (filter.get().equals("all")) {
            return List.copyOf(messages);
        }
        if (!filter.get().equals("unread")) {
            throw new IllegalStateException("Unknown message filter: " + filter.get());
        }
        return messages.stream().filter(Message::unread).toList();
    }

    /// Joins visible message keys in semantic order without an optional intermediate.
    ///
    /// @param visible the visible messages
    /// @return the comma-separated keys
    private static String messageKeys(@Unmodifiable List<Message> visible) {
        StringBuilder result = new StringBuilder();
        for (Message entry : visible) {
            if (!result.isEmpty()) {
                result.append(',');
            }
            result.append(entry.key());
        }
        return result.toString();
    }

    /// Returns a required visible message reaction cell.
    ///
    /// @param key the message key
    /// @return the reaction cell
    private GroupedRuntime.LocalInt reaction(String key) {
        @Nullable GroupedRuntime.LocalInt local = reactionLocals.get(key);
        if (local == null) {
            throw new IllegalStateException("Message has no committed reaction state: " + key);
        }
        return local;
    }

    /// Returns the mounted advanced-settings local cell.
    ///
    /// @return the local cell
    private GroupedRuntime.LocalInt advanced() {
        if (advancedLocal == null || !advancedVisible.get()) {
            throw new IllegalStateException("Advanced settings are not visible");
        }
        return advancedLocal;
    }

    /// Stores immutable message identity and unread state.
    ///
    /// @param key the semantic key
    /// @param unread whether the message is included by the unread filter
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
