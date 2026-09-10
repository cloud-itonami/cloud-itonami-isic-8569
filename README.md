# cloud-itonami-isic-8569

Open Business Blueprint for **ISIC Rev.5 8569**: educational support
activities.

This repository publishes a community-learning-support actor -- learner
intake, educational-support/student-data-privacy assessment, dropout-
risk screening, support-plan finalization and guardian contact -- as an
OSS business that any qualified learning-support operator can fork,
deploy, run, improve and sell, so a community or independent provider
never surrenders learner data and ledgers to a closed SaaS.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691)) --
here it is **Learning Advisor ⊣ Learner Safety Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a
> learner-intake summary, normalizing records, and checking whether a
> learner's own assigned cohort's tutor-load ratio actually stays
> within its own recorded maximum -- but it has **no notion of which
> jurisdiction's educational-support and student-data-privacy law is
> official, no license to finalize a real support plan or contact a
> real guardian, and no way to know on its own whether a dropout risk
> against a learner has actually stayed unresolved**. Letting it
> finalize a support plan or contact a guardian directly invites
> fabricated regulatory citations, a support plan being finalized in
> an already-overloaded tutor cohort, and a dropout risk being quietly
> overlooked -- and liability, and learner-safety risk, for whoever
> runs it. This project seals the LearningOps-LLM into a single node
> and wraps it with an independent **Learner Safety Governor**, a
> human **approval workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers learner intake through educational-support/student-
data-privacy assessment, dropout-risk screening, support-plan
finalization and guardian contact. It does **not**, by itself, hold
any registration required to operate as a community-learning-support
provider in a given jurisdiction, and it does not claim to. It also
does **not** deliver the actual tutoring/instruction itself, or make
placement/discipline decisions -- `learning.registry/learner-to-
tutor-ratio-exceeds-maximum?` is a pure ceiling recompute against the
learner's own recorded fields, not a pedagogical assessment. Whoever
deploys and operates a live instance (a registered learning-support
provider) supplies any jurisdiction-specific registration, the real
tutors/instructors and the real school/family integrations, and bears
that jurisdiction's liability -- the software supplies the governed,
spec-cited, audited execution scaffold so that provider does not have
to build the compliance layer from scratch.

### Actuation

**Finalizing a real support plan or contacting a real guardian is
never autonomous, at any phase, by construction.** Two independent
layers enforce this (`learning.governor`'s `:actuation/finalize-
support-plan`/`:actuation/contact-guardian` high-stakes gate and
`learning.phase`'s phase table, which never puts `:actuation/finalize-
support-plan`/`:actuation/contact-guardian` in any phase's `:auto`
set) -- see `learning.phase`'s docstring and `test/learning/
phase_test.clj`'s `finalize-support-plan-never-auto-at-any-phase`/
`contact-guardian-never-auto-at-any-phase`. The actor may draft, check
and recommend; a human learning-coordinator is always the one who
actually finalizes a support plan or contacts a guardian. Like
`6512`/`6622`/`6520`/`6530`/`6820`/`6920`/`6611`/`8530`/`9200`/`9521`/
`8730`/`9102`/`9103`/`8890`/`8610`/`8510`/`9412`/`8720`/`8521`/`6619`/
`3600`/`6190`/`3030`/`3830`/`9420`/`9491`/`2610`/`3512`/`8810`/`8691`,
this actor has TWO actuation events, both POSITIVE (finalizing/
issuing a real record), matching the majority pattern in this fleet
(`3600`/`6190` are the fleet's two NEGATIVE-actuation exceptions).

## The core contract

```
learner intake + jurisdiction facts (learning.facts, spec-cited)
        |
        v
   ┌──────────────┐   proposal      ┌───────────────────────┐
   │ Learning     │ ─────────────▶ │ Learner Safety                │  (independent system)
   │ Advisor      │  + citations    │ Governor:                    │
   │ (sealed)     │                 │ spec-basis · evidence-       │
   └──────────────┘         commit ◀────┼──────────▶ hold │ incomplete ·
                                 │             │           │ learner-to-tutor-
                           record + ledger  escalate ─▶ human   ratio-exceeds-
                                             (ALWAYS for         maximum (ceiling) ·
                                              :actuation/finalize-       dropout-risk-
                                              support-plan /             unresolved
                                              :actuation/contact-        (unconditional) ·
                                              guardian)                  already-plan-
                                                                          finalized/-contacted
```

**The LearningOps-LLM never finalizes a support plan or contacts a
guardian the Learner Safety Governor would reject, and never does so
without a human sign-off.** Hard violations (fabricated regulatory
requirements; unsupported evidence; a cohort's tutor-load ratio past
its own maximum; an unresolved dropout risk; a double finalization or
contact) force **hold** and *cannot* be approved past; a clean plan/
contact proposal still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean dual-actuation lifecycle + five HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a learning-support robot
performs content delivery, assessment proctoring and assistive
interaction with learners under the actor, gated by the independent
**Learner Safety Governor**. The governor never dispatches hardware
itself; `:high`/`:safety-critical` actions (such as operating near
children or vulnerable learners) require human sign-off.

A live sample of the operator console (robotics safety console, shared
template) is rendered in
[docs/samples/operator-console.html](docs/samples/operator-console.html)
-- pure-data HTML output of `kotoba.robotics.ui`.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Learner Safety Governor, support-plan + guardian-contact draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8569`). This vertical's learner records are practice-specific rather
than a shared cross-operator data contract, so `learning.*` runs on
the generic robotics/identity/forms/dmn/bpmn/audit-ledger stack only
-- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/learning/store.kotoba` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + separate support-plan/guardian-contact history. No dynamically-filed sub-record -- both actuation ops act directly on a pre-seeded learner, and the double-actuation guards check dedicated `:support-plan-finalized?`/`:guardian-contacted?` booleans rather than a `:status` value |
| `src/learning/registry.kotoba` | Support-plan + guardian-contact draft records, plus `learner-to-tutor-ratio-exceeds-maximum?` -- the FIFTH instance of this fleet's ratio-based sufficiency check family (`leasing`/`behavioral`/`union`/`fab` established the first four), applying the MAXIMUM-ceiling direction |
| `src/learning/facts.kotoba` | Per-jurisdiction educational-support/student-data-privacy catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/learning/learningadvisor.kotoba` | **LearningOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/study-plan-verification/dropout-risk-screening/support-plan-finalization/guardian-contact proposals |
| `src/learning/governor.kotoba` | **Learner Safety Governor** -- 4 HARD checks (spec-basis · evidence-incomplete · learner-to-tutor-ratio-exceeds-maximum, pure ground-truth ceiling recompute · dropout-risk-unresolved, unconditional evaluation, the THIRTY-SIXTH grounding of this discipline, a genuinely new concept grounded in this blueprint's own `:dropout-prevention` social-impact tag) + already-plan-finalized/already-guardian-contacted guards + 1 soft (confidence/actuation gate) |
| `src/learning/phase.kotoba` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (both support-plan finalization and guardian contact always human; learner intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/learning/operation.kotoba` | **OperationActor** -- langgraph-clj StateGraph |
| `src/learning/sim.kotoba` | demo driver |
| `test/learning/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers learner intake through educational-support/student-
data-privacy assessment, dropout-risk screening, support-plan
finalization and guardian contact -- the core governed lifecycle this
blueprint's own `docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Learner intake + per-jurisdiction educational-support checklisting, HARD-gated on an official spec-basis citation (`:learner/intake`/`:studyplan/verify`) | Real school/family-system integration, real tutoring/instruction itself (see `learning.facts`'s docstring) |
| Dropout-risk screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:dropout-risk/screen`) | Any pedagogical judgment or placement decision itself -- deliberately outside this actor's competence |
| Support-plan finalization, HARD-gated on full evidence and the learner's own cohort tutor-load ratio, plus a double-finalization guard (`:actuation/finalize-support-plan`) | |
| Guardian contact, HARD-gated on full evidence and consent/purpose-limitation, plus a double-contact guard (`:actuation/contact-guardian`) | |
| Immutable audit ledger for every intake/verification/screening/plan/contact decision | |

Extending coverage is additive: add the next gate (e.g. a scholarship-
eligibility check) as its own governed op with its own HARD checks and
tests, following the SAME "an independent governor re-verifies against
the actor's own records before any real-world act" pattern this
repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`learning.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `learning.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `learning.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `LearningOps-LLM` + `Learner Safety Governor` run as
real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, modeled closely on the fifty-one
prior actors' architecture. See `docs/adr/0001-architecture.md` for
the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
