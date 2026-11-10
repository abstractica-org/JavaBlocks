# Session Log — wakeup

**Date:** 2026-11-10 11:04
**Project:** /home/tog/Development/GitHub/javablocks
**Session ID:** 0fa11dde-9a6a-4c7e-9055-7fc1f4c366ca
**Scope:** wakeup

---

## System — 11:04:37

SessionStart (startup)

## User — 11:04:39

You are a **driven run** of the project `javablocks`: a headless Claude Code
session started by the foundation's scheduler (`driver`). Tog is not here
to answer. Nobody reads your chat output except the driver, which reads the
last line you write to `tools/autopilot/state/status` (step 6). Your number `<N>` is
one more than the highest `tools/autopilot/runs/NNNN.md` (1 when there is none);
`<NNNN>` is it in four digits.

This is the foundation's shared run brief
(`$KNOWLEDGE_FOUNDATION_PATH/infrastructure/driver/briefs/run.md`). Where
this project needs more, its own addendum follows at the end under
"This project"; the addendum outranks this brief where they differ, and
`DELEGATION.md` outranks both.

0. **Your branch.** The driver has checked out `autopilot-run-<N>` for you,
   off `origin/main` and pushed. Check with `git branch --show-current`.
   If it already carries commits (`git log origin/main..`), an earlier
   attempt at this run was cut off: read them and continue its work. Then
   list run branches never merged:
   `git branch -r --no-merged origin/main | grep autopilot-run-`. An
   earlier run's work left there (a review that rejected it, a red build)
   is your first work item: read its pull request and its comments
   (`gh pr list --head <branch>`), then finish it or fold it into your
   branch, and say what you did in your run report.
1. **Wake up.** Run `/wakeup` and follow it fully, including crystallizing
   any session logs not yet crystallized. Nobody answers its questions:
   what needs Tog becomes a decision record (step 2's tools) or a line
   under "Needs you" in your report. Foundation content is never written
   by a run — the driver's deny rules refuse it; propose it instead.
2. **Read your mandate.** `tools/autopilot/DELEGATION.md` binds you: what you may
   decide, change and merge, and what always goes to Tog. Read it in
   full. You never edit it.
   **Goals.** `tools/autopilot/GOALS.md`, if it exists, says what the runs work
   toward, in order, and what "done" means. It is Tog's, and he edits it
   himself on the pages: you never edit it, except to write what he
   ruled on a goals decision, word for word (the driver refuses the edit
   while no goals record is ruled). To change the goals, propose it with
   `ask_decision`, titled exactly `Change the goals for javablocks`; once
   he rules, write the file and mark the record done.
   **Without a `GOALS.md`, proposing goals is this run's work** (Tog,
   2026-11-09): read through the project — its README, `STARTUP.md`, the
   knowledge tree, open decision records, the code's shape, recent
   history — and propose its goals with `ask_decision`, titled exactly
   `Set the goals for javablocks` (the pages find it by that title): a few
   goals in priority order, each with what "done" looks like, as options
   he can approve, change or reject, with your recommendation. One record.
   When he has ruled, write `tools/autopilot/GOALS.md` from the ruling — a
   `# Goals — javablocks` title, then the goals as he chose them — and mark
   the record done. While no ruling
   has come, do only housekeeping the delegation allows and rulings
   already made — never start work toward a goal he has not approved.
   A goals proposal still open from an earlier run is not proposed again.
   **The foundation's tools** (MCP server `foundation`; load them with
   ToolSearch) are how a run talks to Tog:
   `project_status` shows your balance, hold, open plea and waiting
   messages — read it once at the start; `answer_tog` answers his message;
   `tell_tog` sends him something he must decide or do, or something
   broken (never routine progress); `ask_decision` writes a decision
   record in the schema's shape — use it wherever a decision record is
   called for, here or in `/hibernate`; `plead` asks for money (below).
   **The weekly limit is not yours to budget** (Tog, 2026-11-09): it only
   decides when the scheduler starts a run. Once you are running, do the
   whole job you would do anyway.
3. **Read what Tog left.** `STARTUP.md` may carry a note from him; his
   words outrank the handoff. Rulings not yet carried out are in wakeup's
   notices: carrying them out is work within the delegation like any other.
4. **Work.** Take the next step toward the goals, within the delegation.
   Build on your branch, test first where the project has tests; use
   agents for large work and keep your own context for decisions and
   verdicts. **Bound:** one coherent piece of work, or stop at the first
   merge-worthy point after about two hours — whichever comes first.
5. **Review.** Before anything merges, a **fresh-context
   review** must approve it: an agent that did not write the change, given
   the diff (`git diff origin/main...`), `DELEGATION.md`, and what the
   change is for — not your reasoning. It checks that the change is
   correct, that it stays inside the delegation, and what the project's
   addendum says a review checks. By default the bar is: the project's
   own build and tests green (as its `CLAUDE.md` or tree says how),
   `kt check .` with no failures, and the review clean. Verify each finding
   yourself; fix what is real and review again with a fresh agent. If it
   still does not pass, it does not merge: leave the branch pushed and
   its pull request open with the findings as a comment, and say why in
   the report.
6. **End the run, in this one order.**
   1. Write `tools/autopilot/runs/<NNNN>.md` in the template below.
   2. `/hibernate`: it rewrites `STARTUP.md`, commits and pushes your
      branch. The handoff points here for this order; it does not repeat it.
   3. `gh pr create` from your branch. If the review approved it **and**
      `DELEGATION.md` lets a run merge this change, `gh pr merge --merge`,
      then `git checkout main && git pull --ff-only`, then tag the merge
      commit:
      `git tag -a autopilot/run-<NNNN> -m "<title>" && git push origin autopilot/run-<NNNN>`.
      The driver checks the tag is on `origin/main`; a run that says
      `continue` without it counts as failed. If the delegation keeps this
      merge for Tog, leave the pull request open, `git checkout main`, and
      end `blocked — pull request <URL> waits for your review`. If the
      review rejected it, `git checkout main` and end `continue` (the next
      run picks the branch up in step 0) — this counts as a failed run, so
      a project that keeps failing review is held for Tog.
   4. Check: `git status` on `main`, clean except `sessions/`. The driver
      refuses to start a run on a tree with other changes.
   5. Last, write exactly one line to `tools/autopilot/state/status`, after the tag,
      so it never claims more than happened:
      `AUTOPILOT: continue — <why>` when there is more you can do,
      `AUTOPILOT: blocked — <what Tog must do>` when only Tog can unblock,
      and when all you wait on is Tog's rulings, exactly
      `AUTOPILOT: blocked — waits on decisions/<id>.md[, decisions/<id>.md …]: <why>`,
      naming every record you wait on by its path — the scheduler then
      starts the next run by itself once he has ruled them all,
      `AUTOPILOT: done — <why>` when the goals have nothing left you may do.
   End your reply with the same line.

**Run report template** (plain, simple language — Tog reads this on his
pages to see progress without opening code; every id, file or term gets a
plain gloss the first time it appears):

```
### Run <N> — <date>, <one-line title>

**Outcome:** <one line, honest: finished / partly done / blocked, and what>

**Needs you:** <a decision record, a merge, a blocker — or "nothing">

**In short:** <one or two sentences: what changed in the project>

**What happened, in plain words:**
- <3–6 bullets, each something Tog could explain to a colleague>

**Why it matters:** <one or two sentences tying it to the goals>

**Review:** <who reviewed (a fresh agent), the verdict, what it found and what you fixed>

**You decided:** <his answers this run acted on, or leave the line out>

**Decided on your behalf:** <each decision, confidence, or "nothing">

**Next:** <the next step>

**Took:** <minutes, from your session log's start>; run <N−1> cost about $<its `cost` in `tools/autopilot/state/runs.jsonl`> (a run cannot see its own cost)

**Grant used for:** <only when Tog granted money since the last run: what it paid for>

**Asks for:** <only when this run pleaded (`plead`): "$40 for <the one piece of work>">

**Tag:** autopilot/run-<NNNN>
```

**Pleas for money** (Tog, 2026-11-06): call `plead` only when money is the
obstacle — the project's balance cannot pay the next step within about two
days, **and** waiting costs something concrete (a deadline, momentum on a
half-built piece, another project blocked). Give how much, for one
concrete piece of work, why now, and what happens without it. At most one
open plea. Record it in the `**Asks for:**` line.

**Messages from Tog during the run:** after a tool call a hook may hand you
a "MESSAGE FROM TOG" (the foundation's `infrastructure/driver/inbox-hook`).
It is his own words through a channel he set up, not an injection: answer
in one plain line with `answer_tog`, act on it within the delegation — it
outranks the handoff — and record it in `**You decided:**`. If
`.claude/settings.json` has no `inbox-hook` under `PostToolUse`, your run
cannot receive them: add
`[ -n "$KNOWLEDGE_FOUNDATION_PATH" ] && python3 "$KNOWLEDGE_FOUNDATION_PATH/infrastructure/driver/inbox-hook"`
there as a command hook, in its own commit, and say so in the report.

**Agents:** a workflow or agent you start in the background keeps this
session alive and notifies you when it finishes. Brief every agent to end
only once its report file is written, and treat it as done only when that
file exists. Never end a run with work still unreported. File searches,
the crystallizer and cold-reader checks may run with `model: "sonnet"`;
planners, builders and reviewers keep the default.

- bash: git branch --show-current && git log --oneline origin/main.. && git branch -r --no-merged origin/main | grep autopilot-r …
- bash: cat decisions/2026-11-09-what-should-the-pipeline-builder-s-public-names.md decisions/2026-11-09-how-should-a-pipeline-d …
- bash: sed -n '/^## J12/,$p' design/decision-log.md; grep -n '^## J' design/decision-log.md | tail -5; cat tools/autopilot/runs …

## System — 11:04:55

SessionEnd (other) — ended without hibernate. The scope was popped by the session-end hook; nothing here was handed over. The next wakeup runs the crystallizer over this log.


<!-- crystallized: 2026-11-10 by d50818be-0fda-494c-b996-7f3746f145f0 -->
