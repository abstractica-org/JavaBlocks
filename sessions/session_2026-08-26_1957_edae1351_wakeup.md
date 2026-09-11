# Session Log — wakeup

**Date:** 2026-08-26 19:57
**Project:** /home/tog/Development/GitHub/javablocks
**Session ID:** edae1351-a327-48b6-bff1-6c6d77d32bab
**Scope:** wakeup

---

## User — 19:57:25

/wakeup

## Assistant — 19:58:10

Everything resolved — no missing skills, tools, or practices. The only files under `sessions/` newer than `STARTUP.md` are this session's own log (`…_edae1351_wakeup.md`, opened 19:57 by the hook), so the handoff is fresh, not stale.

```
Mode: project  (cwd ≠ foundation)
foundation: /home/tog/Development/GitHub/knowledge-foundation
└── knowledge/meta/                       — loaded fully
    ├── architecture/  schema/  placement-guide/  commands/  consumers/
    └── rules/ 01-foundation · 02-session · 03-working-with-content
               · 04-organization · 05-communication   (all 16 rules)

.profile.yml  (archetype: link)
├── skills/
│   ├── java-library — plain Java (LTS) + Maven + JUnit 5, no framework
│   │   ├── tools: language/java · language/maven · testing/junit
│   │   └── practices: integration/real-integrations
│   └── github-vcs — git local, GitHub remote
│       └── tools: vcs/git · vcs/github
└── domains/
    ├── ai-collaboration — declare-before-doing, knowledge-transfer,
    │     record-rejected-alternatives, verify-before-done,
    │     generate-then-verify, scoped-edits, coverage-as-feature,
    │     small-steps, no-secrets-in-output
    └── software-engineering — atomic-commits, feature-branches,
          no-backward-compat-hacks, interface-first, plumbing-and-porcelain,
          check-and-fix-one-rule, fail-loud-at-boundaries,
          no-validation-of-impossible, trust-internal-code,
          test-where-components-meet, probe-the-seam,
          prove-the-test-can-fail, kill-the-tree-not-the-process

javablocks/knowledge/
└── _index.md — orientation only (model in one paragraph, where things
    are); no child topics yet

STARTUP.md (2026-08-26 19:53, fresh) — hands this session:
  code pinned at v0.2.0, no tests, template pom. Scope, ONE at a time:
  1. fix the pom (artifactId javablocks, org.abstractica, 0.3.0-SNAPSHOT,
     Java pinned — owner chooses, JUnit 5), build green;
  2. characterization tests of v0.2.0 warts-included, prove each can fail,
     tag v0.2.1;  3. ruled cleanups;  4. owner-designed declarative
     assembly;  5. contract adapter (only after wiring-playground's contract).
  Read design/decision-log.md J1 first.
```

Rules loaded and active. Foundation at `/home/tog/Development/GitHub/knowledge-foundation`. Project tree at `javablocks/knowledge/` loaded. Use `/hibernate` to close the session.

Step 1 (the pom) has one decision that's yours: the Java version. `STARTUP.md` notes 25 is the house standard; the template currently says 22. Say the word and I'll start there.

## User — 21:19:09

Java 25, fix the pom

## Assistant — 21:20:22

Step 1 done.

**What changed** — `pom.xml` is now the real thing, in the same form as `sandbox`/`ai-persona`:
- `org.abstractica:javablocks:0.3.0-SNAPSHOT`, `jar`, with name/description
- Java 25 pinned via `maven.compiler.release` (the old pom used `source`/`target` 22)
- JUnit 5 via `junit-bom` 5.11.4 + test-scoped `junit-jupiter`; compiler 3.13.0 and surefire 3.5.2 pinned in `pluginManagement`
- `distributionManagement` → GitHub Packages for `abstractica-org/JavaBlocks` (sibling convention; nothing published)

**Verified** — `mvn clean verify` green on JDK 25.0.4 / Maven 3.8.7; all 62 sources compile to `target/javablocks-0.3.0-SNAPSHOT.jar`, class files at major version 69 (= Java 25). No `src/` change. The only build noise is Maven 3.8.7's bundled Guava tripping `sun.misc.Unsafe` deprecation warnings on JDK 25 — harmless, not ours.

**Logged** — `design/decision-log.md` **J2** records the coordinates, the Java 25 ruling, and the JUnit addition. `CLAUDE.md`'s status line and Profile note updated: the `java-library` claim is now verified against the build, as the note asked.

**Committed** as `9ea05f9` on a new branch `pom-java25` (branched off `main` per the feature-branch practice; not pushed). If you'd rather keep this repo's direct-to-`main` history, say so and I'll fast-forward `main` instead.

One thing I noticed and left alone: `sessions/` (the hook's transcript logs) shows as untracked — there's no `.gitignore` entry for it. Whether it's meant to be committed or ignored is your call.

Next is step 2, the characterization tests against `v0.2.0` — ready when you are.

## User — 21:21:03

Merge to main, then start the characterization tests

## Assistant — 21:45:41

Step 2 is done and tagged. Recap:

**Tests** — 34 characterization tests, six classes under `src/test/java/org/abstractica/javablocks/blocks/basic/` (Buffer, ThreadBlock, Distributor, If, Map, function blocks + timer idiom) plus `testsupport/` (`Sink`, `BlockingSink`, `Async`). `src/main` is byte-identical to `v0.2.0`. All of STARTUP's list is pinned; each test was proven able to fail — 13 one-line library mutations (put never blocks, stop ignores the in-flight put, post-put lock removed, distributor reversed/unlocked, if-check removed, new map handler per put, handler put under the lock, delay as one sleep, …) each turned exactly the claiming tests red, then reverted. Three consecutive full runs green, ~9 s.

**The big surfaced wart** — the first run hung, and the thread dump showed why: `stop()` is `synchronized` and `join()`s *holding the monitor*, while the worker needs that monitor for its post-put `notifyAll()`. So `stop()` completes only when the worker is parked inside an *interruptible* `get()` (a buffer). With `ConstantBlock`, a `DelayFunction` on either side, or a socket, it either re-waits in 5 s rounds forever or wins the microsecond window and deadlocks — and every later `isRunning()` blocks too. **`FirstTest`'s own timer idiom is unstoppable.** Pinned deterministically (`stopDeadlocksWithAWorkerWhoseGetSurvivesTheInterrupt` + two timer tests). Also pinned: a downstream `RuntimeException` kills the worker while `isRunning()` stays true; `DelayFunction` swallows interrupts; capacity-0 buffers block every put; the distributor forwards under its monitor.

**Record** — `design/decision-log.md` **J3** (what's pinned, the five new warts, the rejected "silence reporters globally" with its revival trigger); `CLAUDE.md` status and cleanup list updated. Commit `72dd3ca`, tag **`v0.2.1`**. Committed straight to `main` this time since you'd asked for main; nothing pushed. `sessions/` is still untracked — your call whether to ignore or commit it.

**Next (step 3, one ruled step each)** — the cleanup list now has a clear first candidate: the `stop()` deadlock, since it makes the timer idiom (and any socket-fed thread) unstoppable. My recommendation when you want it: release the monitor before `join()` and make the worker's `notifyAll` not depend on it — but that's yours to rule, and it's a visible change to three tests.

## System — 10:47:47

SessionEnd (cleanup) — ended without hibernate. The scope was popped by the session-end hook; nothing here was handed over. The next wakeup runs the crystallizer over this log.

