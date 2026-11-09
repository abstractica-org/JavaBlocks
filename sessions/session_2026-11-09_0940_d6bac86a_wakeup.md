# Session Log — wakeup

**Date:** 2026-11-09 09:40
**Project:** /home/tog/Development/GitHub/javablocks
**Session ID:** d6bac86a-7821-41fe-bd6f-c1d670e720de
**Scope:** wakeup

---

## System — 09:40:33

SessionStart (startup)

## User — 09:40:35

You are a one-off **migration run** in the project `javablocks` (abstractica-org/JavaBlocks),
started by the foundation's scheduler because Tog clicked "Migrate" on his
project pages. You work unattended; nobody answers questions during the
run. You are on the branch `migrate-20261109-0940`, made from `origin/main`; the
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
8. **Commit, push, pull request.** Commit everything on `migrate-20261109-0940`, push
   it, and open the pull request against `main` with
   `gh pr create --base main --head migrate-20261109-0940 --title "Migration to the
   foundation" --body-file <your report>`. The report, in
   plain words for Tog, in this order: **In short** (two lines); **Changed**
   (what, per file group); **Left for later** (rulings needing code,
   warnings, anything odd you saw); **The delegation question** (its
   file); **Cost of this run is visible on the pages**.
9. **Last**, write exactly one line to `/home/tog/.local/state/project-driver/jobs/javablocks/status`:
   `AUTOPILOT: done — <the pull request's URL>`, or, if you could not open
   a pull request, `AUTOPILOT: blocked — <why, in one line>`.

Work carefully and briefly; this run is paid from Tog's pool.

- bash: git status --short && ls && ls decisions docs sessions .claude 2>&1; $KNOWLEDGE_FOUNDATION_PATH/infrastructure/kt/kt che …
- bash: cat README.md; echo ====; cat decisions/*.md; echo ====; ls $KNOWLEDGE_FOUNDATION_PATH/knowledge/meta/schema/decision-re …
- bash: cat $KNOWLEDGE_FOUNDATION_PATH/knowledge/meta/schema/decision-records/_index.md $KNOWLEDGE_FOUNDATION_PATH/knowledge/met …
- bash: cat pom.xml | head -60; find src -name '*.java' | sed 's|src/main/java/org/abstractica/javablocks/||' | sort | head -80; …
- bash: cd /home/tog/Development/GitHub/javablocks && mkdir -p docs && git mv README.md docs/original-readme.md && cat > README. …
- bash: python3 - <<'EOF' …
- bash: cd $KNOWLEDGE_FOUNDATION_PATH/infrastructure/decide && ls; grep -n '"draft"\|\["\|\.get("' decide* *.py 2>/dev/null | he …
- bash: cd $KNOWLEDGE_FOUNDATION_PATH/infrastructure/decide && sed -n 20,60p decide; sed -n 115,125p test_decide.py; sed -n 150, …
- bash: cat > /tmp/jb-deleg.py <<'PYEOF' …
- bash: python3 - <<'EOF' …
