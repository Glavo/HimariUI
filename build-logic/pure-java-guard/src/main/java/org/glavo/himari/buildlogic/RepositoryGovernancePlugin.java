package org.glavo.himari.buildlogic;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;
import java.util.List;

/// Installs machine-enforced governance checks on the root HimariUI project.
@NotNullByDefault
public final class RepositoryGovernancePlugin implements Plugin<Project> {
    /// Creates a repository governance plugin instance.
    public RepositoryGovernancePlugin() {
    }

    /// Applies the governance checks and attaches their aggregate task to `check`.
    ///
    /// @param project the project receiving the plugin
    @Override
    public void apply(Project project) {
        if (project != project.getRootProject()) {
            throw new GradleException("The repository governance plugin must be applied only to the root project");
        }

        Path root = project.getProjectDir().toPath();
        TaskProvider<Task> adr = verificationTask(
                project,
                "verifyAdrCatalog",
                "Validates the canonical ADR catalog and repository policy documents.",
                () -> RepositoryGovernanceValidator.verifyAdrCatalog(root)
        );
        TaskProvider<Task> references = verificationTask(
                project,
                "verifyReferencesLock",
                "Validates the reference lock and its exact coverage of the plan bibliography.",
                () -> RepositoryGovernanceValidator.verifyReferencesLock(root)
        );
        TaskProvider<Task> provenance = verificationTask(
                project,
                "verifyProvenanceManifest",
                "Validates provenance records, tracked payload coverage, and source hashes.",
                () -> RepositoryGovernanceValidator.verifyProvenanceManifest(root)
        );
        TaskProvider<Task> conformance = verificationTask(
                project,
                "verifyPlatformConformance",
                "Validates the platform conformance register and waiver state.",
                () -> RepositoryGovernanceValidator.verifyPlatformConformance(root)
        );

        TaskProvider<Task> aggregate = project.getTasks().register("verifyRepositoryGovernance", task -> {
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            task.setDescription("Runs every active repository governance check.");
            task.dependsOn(List.of(adr, references, provenance, conformance));
        });
        project.getPlugins().withType(
                LifecycleBasePlugin.class,
                ignored -> project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME)
                        .configure(task -> task.dependsOn(aggregate))
        );
    }

    /// Registers a repository validation task.
    ///
    /// @param project the task-owning root project
    /// @param name the task name
    /// @param description the task description
    /// @param action the validation action
    /// @return the registered task provider
    private static TaskProvider<Task> verificationTask(
            Project project,
            String name,
            String description,
            Runnable action
    ) {
        return project.getTasks().register(name, task -> {
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            task.setDescription(description);
            task.notCompatibleWithConfigurationCache(
                    "The validation reads repository documents and may scan tracked source resources."
            );
            task.doLast(ignored -> action.run());
        });
    }
}
