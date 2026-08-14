# Architecture Decision Records

This directory is the canonical decision register for HimariUI. `PLANS.md` supplies roadmap context, but implementation must follow the status and decision recorded here.

ADR statuses are:

- `Proposed`: a requirement or candidate awaiting the named evidence and decision milestone;
- `Accepted`: the decision governs implementation;
- `Superseded`: a later ADR replaces the decision;
- `Rejected`: the candidate was evaluated and rejected.

Every ADR records its date, decision milestone, evidence, and replacement relationship. Materially changing an accepted decision requires a new ADR; update `Superseded by` in the old record and `Supersedes` in the new record.

ADR-001 through ADR-019 and ADR-022 were accepted by ADR-BOOTSTRAP-001 on 2026-08-14 from the reviewed execution summaries in `PLANS.md`. ADR-020 and ADR-021 remain proposed until their M1 evidence requirements pass.

Use `TEMPLATE.md` for new records. The repository governance check validates the catalog, required metadata and sections, accepted/proposed status boundary, and relative Markdown links.
