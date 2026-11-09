# Session Log — wakeup

**Date:** 2026-11-09 09:59
**Project:** /home/tog/Development/GitHub/javablocks
**Session ID:** 51fdfa47-62d7-4f3b-ae77-bba21a9ef6eb
**Scope:** wakeup

---

## System — 09:59:46

SessionStart (startup)

## User — 09:59:48

You are a one-off **migration run** in the project `javablocks` (abstractica-org/JavaBlocks),
started by the foundation's scheduler because Tog clicked "Migrate" on his
project pages. You work unattended; nobody answers questions during the
run. You are on the branch `migrate-20261109-0959`, made from `origin/main`; the
scheduler has already added any missing foundation ceremony with
`bootstrap wire` (uncommitted — review it and commit it with your work).

**Your job is housekeeping, so that this project fits the foundation as it
is today.** You end with one pull request that Tog reviews. You never
merge it, never push to `main`, and never change the project's own code.

## What you may change

Only these: `decisions/`, `knowledge/`, `CLAUDE.md`, `STARTUP.md`,
`README.md`, `docs/`, `.claude/`, `.profile.yml`, `.gitignore`,
and `sessions/`. **Nothing else** — no source code, build files, tests, content, configuration of
the project's product. In `docs/` you only *add* files (the old README,
moved); you never edit documentation that is already there. If
something outside these needs changing, say so in the report instead.

## Steps

1. **Wake up.** The session pack was injected at start; read its Notices
   first. Run `$KNOWLEDGE_FOUNDATION_PATH/infrastructure/kt/kt check .`
   and keep its output for the report.
2. **Decision records in an old shape** (the notices name them): rewrite
   each into the shape `foundation:knowledge/meta/schema/decision-records/`
   fixes — frontmatter, Problem, Options each with **Pros:** and **Cons:**
   lists, Recommendation, If nothing is decided — keeping every argument
   and any `## Ruling`, `## Deferred` or `## Done` exactly as written. Do
   not change what a record says, only its shape; where the shape needs
   something the record never said (a con, say), write what follows
   plainly from its own text, and mark it "(added in migration)".
3. **Rulings not yet carried out** (the notices name them): if carrying
   one out needs only the files you may change, do it and append
   `## Done`. If it needs code or anything else, leave it `ruled` and list
   it in the report as work for a later run or for Tog.
4. **The front page, `README.md`.** Read the project first — its code
   (entry points, controllers, `main`, the build file), `CLAUDE.md`,
   `STARTUP.md` and the knowledge tree — and write the README as
   accurately as you can, in the shape
   `foundation:knowledge/meta/schema/readme/` fixes: the title; one or
   two paragraphs on what it is, what it does, why it exists and for
   whom; `## Status` (status word in bold, where it stands, today's
   date); an optional `## Quick start`; `## Documentation` linking what
   exists. At most 400 words. Write it for a stranger: some repos are
   public, so no balances, session ids or quoted rulings. Every claim
   is one you checked in the repo; if you could not tell, leave it out.
   **If a README already exists** and does not fit the shape, move it
   word for word with `git mv README.md docs/user-manual.md` (another
   name under `docs/` if that one is taken, and a name that fits if it
   is not a manual), then write the new one and link the moved file
   first under Documentation. Fix any relative links in the moved file
   that the move broke — that is the one edit you may make to it.
5. **`kt check` green** (warnings may stay; say which). Fix what the files
   you may change can fix.
6. **Draft the delegation and ask Tog to approve it.** The draft is the
   whole of `tools/autopilot/DELEGATION.md` as it is to be written: a
   `# Delegation — javablocks` title, then what a driven run in this
   project could decide on its own, and what always goes to Tog. Ground
   it in this project — its kind, its users, what is expensive to undo —
   in plain words, half a page. **Do not create the file yourself**:
   approving the record writes it. Ask with
   `$KNOWLEDGE_FOUNDATION_PATH/infrastructure/decide/decide ask --dir .`,
   titled exactly `Approve the autopilot delegation for javablocks`, with
   the draft — and only the draft — in the JSON's `"draft"` field (never
   in `"problem"`). `decide` puts it at the end of the Problem under the
   fixed heading `### The draft`, in one fenced block, and refuses the
   question without it. The rest follows the schema's four steps; the
   options include at least `Approve as drafted` (that exact name:
   ruling it writes the file), `Keep the project parked` and
   `Approve with changes`, each with pros and cons, then your
   recommendation and confidence. The project stays parked until he
   answers.
7. **The handoff.** Update `STARTUP.md` (or create it) so the next session
   knows the project was migrated, what is open, and where the delegation
   question is.
8. **Commit, push, pull request.** Commit everything on `migrate-20261109-0959`, push
   it, and open the pull request against `main` with
   `gh pr create --base main --head migrate-20261109-0959 --title "Migration to the
   foundation" --body-file <your report>`. The report, in
   plain words for Tog, in this order: **In short** (two lines); **Changed**
   (what, per file group); **Left for later** (rulings needing code,
   warnings, anything odd you saw); **The delegation question** (its
   file); **Cost of this run is visible on the pages**.
9. **Last**, write exactly one line to `/home/tog/.local/state/project-driver/jobs/javablocks/status`:
   `AUTOPILOT: done — <the pull request's URL>`, or, if you could not open
   a pull request, `AUTOPILOT: blocked — <why, in one line>`.

Work carefully and briefly; this run is paid from Tog's pool.

- bash: git status --short && git log --oneline -6 && ls -a && ls decisions docs tools tools/autopilot 2>&1; $KNOWLEDGE_FOUNDATI …
- bash: for f in decisions/*; do echo "=== $f"; cat "$f"; done; echo ===; cat tools/autopilot/DELEGATION.md; git log --oneline - …
- bash: git fetch -q origin && git log --oneline origin/main..main | head; git log --oneline main..origin/main | head; git tag - …
- bash: git tag; git rev-parse --abbrev-ref HEAD; git log --oneline -1 v0.2.1 2>&1; git branch -a --contains 92db375 | head
- bash: python3 - <<'EOF' …
- bash: python3 - <<'EOF' …
- bash: sed -n 40,46p STARTUP.md && $KNOWLEDGE_FOUNDATION_PATH/infrastructure/kt/kt check . 2>&1 | tail -3 && cat > /tmp/javablo …
