package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Objects;

/// Publishes a group of source-state writes atomically as at most one domain epoch.
///
/// Transactions execute synchronously on the [StateDomain] owner thread. A nested transaction for
/// the same domain is flattened into its outer transaction. Each nested call has a savepoint: if it
/// throws, only writes staged since that nested call began are rolled back before the failure is
/// rethrown. If the outermost call throws, all staged writes are discarded. Transactions for
/// different domains must not overlap on one thread.
@NotNullByDefault
public final class StateTransaction {
    /// The transaction currently active on each execution thread.
    private static final ThreadLocal<@Nullable TransactionContext> CURRENT = new ThreadLocal<>();

    /// Prevents instantiation of this utility class.
    private StateTransaction() {
    }

    /// Runs an action in an atomic state transaction.
    ///
    /// A successful action that leaves every source semantically unchanged does not advance the
    /// domain epoch. An exception or error is rethrown after the applicable savepoint is restored.
    ///
    /// @param domain the domain to mutate
    /// @param action the synchronous action to run
    /// @throws IllegalStateException if called outside the domain owner thread or while another
    /// domain has an active transaction on this thread
    public static void run(StateDomain domain, Runnable action) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(action, "action");
        domain.checkOwnerThread();
        domain.checkWriteAllowed();

        @Nullable TransactionContext active = CURRENT.get();
        if (active != null) {
            if (active.domain() != domain) {
                throw new IllegalStateException("Transactions for different state domains cannot overlap");
            }
            active.runNested(action);
            return;
        }

        TransactionContext context = new TransactionContext(domain);
        CURRENT.set(context);
        try {
            action.run();
            domain.commit(context.stagedValues());
        } finally {
            CURRENT.remove();
        }
    }

    /// Returns a source's transaction-visible or latest published value.
    ///
    /// @param source the source to read
    /// @return the visible value, which may be `null`
    static @Nullable Object read(AbstractStateSource source) {
        @Nullable TransactionContext active = CURRENT.get();
        if (active != null && active.domain() == source.owningDomain() && active.contains(source)) {
            return active.value(source);
        }
        return source.owningDomain().currentPublication().value(source.slot());
    }

    /// Stages a source write, opening an implicit transaction when necessary.
    ///
    /// @param source the source to mutate
    /// @param value the validated replacement value, which may be `null`
    static void write(AbstractStateSource source, @Nullable Object value) {
        StateDomain domain = source.owningDomain();
        domain.checkOwnerThread();
        domain.checkWriteAllowed();
        @Nullable TransactionContext active = CURRENT.get();
        if (active == null) {
            run(domain, () -> requireCurrent(domain).stage(source, value));
            return;
        }
        if (active.domain() != domain) {
            throw new IllegalStateException("Cannot write a source from another active transaction domain");
        }
        active.stage(source, value);
    }

    /// Rejects source registration while any transaction is active on this thread.
    ///
    /// @param domain the domain receiving the source
    /// @throws IllegalStateException if a transaction is active
    static void checkRegistrationAllowed(StateDomain domain) {
        @Nullable TransactionContext active = CURRENT.get();
        if (active != null) {
            String relation = active.domain() == domain ? "this domain" : "another domain";
            throw new IllegalStateException(
                    "Cannot register a state source while a transaction for " + relation + " is active"
            );
        }
    }

    /// Rejects an operation that requires an outer transaction boundary while a transaction is active.
    ///
    /// @param domain the operation's domain
    /// @param operation the diagnostic operation name
    /// @throws IllegalStateException if a transaction is active on this thread
    static void checkNoActiveTransaction(StateDomain domain, String operation) {
        @Nullable TransactionContext active = CURRENT.get();
        if (active != null) {
            String relation = active.domain() == domain ? "this domain" : "another domain";
            throw new IllegalStateException(
                    operation + " cannot run while a transaction for " + relation + " is active"
            );
        }
    }

    /// Returns whether this thread has an active transaction for a domain.
    ///
    /// @param domain the candidate domain
    /// @return whether a matching transaction is active
    static boolean isActive(StateDomain domain) {
        @Nullable TransactionContext active = CURRENT.get();
        return active != null && active.domain() == domain;
    }

    /// Returns the current transaction after verifying its domain.
    ///
    /// @param domain the expected domain
    /// @return the active context
    /// @throws IllegalStateException if no matching context is active
    private static TransactionContext requireCurrent(StateDomain domain) {
        @Nullable TransactionContext active = CURRENT.get();
        if (active == null || active.domain() != domain) {
            throw new IllegalStateException("Expected an active state transaction");
        }
        return active;
    }

    /// Stores final staged values for one outermost transaction.
    @NotNullByDefault
    private static final class TransactionContext {
        /// The domain being mutated.
        private final StateDomain domain;

        /// The final staged value for each written source.
        private final IdentityHashMap<AbstractStateSource, @Nullable Object> stagedValues;

        /// Creates an empty transaction context.
        ///
        /// @param domain the transaction domain
        private TransactionContext(StateDomain domain) {
            this.domain = domain;
            this.stagedValues = new IdentityHashMap<>();
        }

        /// Returns the transaction domain.
        ///
        /// @return the domain
        private StateDomain domain() {
            return domain;
        }

        /// Returns the mutable staged-value map owned by this context.
        ///
        /// @return the staged values
        private IdentityHashMap<AbstractStateSource, @Nullable Object> stagedValues() {
            return stagedValues;
        }

        /// Returns whether this transaction has a staged value for a source.
        ///
        /// @param source the source
        /// @return whether a value is staged
        private boolean contains(AbstractStateSource source) {
            return stagedValues.containsKey(source);
        }

        /// Returns a source's staged value.
        ///
        /// @param source the source with a staged value
        /// @return the staged value, which may be `null`
        private @Nullable Object value(AbstractStateSource source) {
            return stagedValues.get(source);
        }

        /// Replaces the final staged value for a source.
        ///
        /// @param source the source
        /// @param value the replacement value, which may be `null`
        private void stage(AbstractStateSource source, @Nullable Object value) {
            stagedValues.put(source, value);
        }

        /// Runs a nested action with rollback to its entry savepoint on failure.
        ///
        /// @param action the nested action
        private void runNested(Runnable action) {
            IdentityHashMap<AbstractStateSource, @Nullable Object> savepoint =
                    new IdentityHashMap<>(stagedValues);
            try {
                action.run();
            } catch (RuntimeException | Error failure) {
                stagedValues.clear();
                stagedValues.putAll(savepoint);
                throw failure;
            }
        }
    }
}
