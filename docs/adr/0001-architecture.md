# ADR-0001: Learning Advisor ⊣ Learner Safety Governor architecture

## Status

Accepted. `cloud-itonami-isic-8569` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-8569` publishes an OSS business blueprint for
community learning support: tutoring operations, attendance follow-up,
scholarship guidance and school-community case management. Like every
prior actor in this fleet, the blueprint alone is not an
implementation: this ADR records the governed-actor architecture that
promotes it to real, tested code, following the same langgraph-clj
StateGraph + independent Governor + Phase 0→3 rollout pattern
established by `cloud-itonami-isic-6511` (life insurance) and applied
across fifty-one prior siblings, most recently `cloud-itonami-isic-
8691` (health access navigation).

## Decision

### Decision 1: entity and op shape

The primary entity is a `learner`. Five ops: `:learner/intake`
(directory upsert, no capital risk), `:studyplan/verify` (per-
jurisdiction educational-support/student-data-privacy evidence
checklist, never auto), `:dropout-risk/screen` (dropout-risk
screening, unconditional-evaluation discipline, never auto),
`:actuation/finalize-support-plan` (POSITIVE, high-stakes -- finalizing
a real support plan for a learner), and `:actuation/contact-guardian`
(POSITIVE, high-stakes -- contacting a real learner's guardian). This
matches the dual-actuation-on-one-entity shape every recent dual-
actuation sibling uses, grounded directly in this blueprint's own
published Core Contract ("The advisor can draft support plans, but
cannot expose learner data, make high-stakes placement decisions or
contact guardians without policy approval") and Offer ("study-plan
generation with human review", "guardian communication queue").

### Decision 2: `learner-to-tutor-ratio-exceeds-maximum?` -- the 5th ratio-based sufficiency check, MAXIMUM direction

Following `leasing.registry/collateral-coverage-ratio-insufficient?`
(1st, MINIMUM-floor direction), `behavioral.registry/supervision-
ratio-insufficient?` (2nd, MAXIMUM-ceiling direction), `union.
registry/strike-vote-share-insufficient?` (3rd, MINIMUM-floor
direction again) and `fab.registry/yield-rate-insufficient?` (4th,
MINIMUM-floor direction again), `learning.registry/learner-to-tutor-
ratio-exceeds-maximum?` applies the SAME quotient-comparison shape --
the MAXIMUM direction, like `behavioral`'s -- to a learner's own
assigned cohort's learner count divided by its own tutor count, which
must not exceed a fixed policy ceiling (`maximum-tutor-load-ratio`, 12
learners per tutor, the same honestly-documented starting-simplification
discipline `behavioral.registry/maximum-supervision-ratio` uses) --
a direct, natural mapping onto real tutoring-quality-of-service
practice. Gates only `:actuation/finalize-support-plan`.

### Decision 3: `dropout-risk-unresolved-violations` -- the 36th unconditional-evaluation screening grounding, a genuinely new concept

Before writing this check, every prior sibling's `governor.cljc` was
grepped for `dropout` -- ZERO hits, confirming this is a genuinely new
concept (not a reuse), avoiding the false-precedent-claim risk
`leasing`'s ADR-0001 documents and applying the verification
discipline `union`'s, `congregation`'s, `fab`'s, `care`'s and
`navigator`'s own ADR-0001s established. `dropout-risk-unresolved-
violations` reuses the unconditional-evaluation DISCIPLINE
(`casualty.governor/sanctions-violations`'s original fix) for the
36th distinct application overall, continuing the count established
across this window's builds (water=25th, telecom=26th, aerospace=
27th, recovery=28th, consulting=29th, union=30th, congregation=31st,
fab=32nd, energy=33rd, care=34th, navigator=35th, learning=36th).
Grounded directly in this blueprint's own `:itonami.blueprint/social-
impact` tag `:dropout-prevention`. Gates `:dropout-risk/screen` and
`:actuation/finalize-support-plan` specifically -- a support plan
should not be finalized while a learner's own dropout risk remains
unaddressed.

### Decision 4: dedicated double-actuation-guard booleans

`:support-plan-finalized?`/`:guardian-contacted?` are dedicated
booleans on the `learner` record, never a single `:status` value --
the same discipline every prior sibling governor's guards establish,
informed by `cloud-itonami-isic-6492`'s real status-lifecycle bug
(ADR-2607071320).

### Decision 5: Store protocol, MemStore + DatomicStore parity

`learning.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in `test/learning/
store_contract_test.clj` -- the same seam every sibling actor uses so
swapping the SSoT backend is a configuration change, not a rewrite.
The protocol's per-entity accessor is named `learner` directly --
`learner` is not a Clojure special form, so no `-of` suffix workaround
was needed (that workaround, confirmed unnecessary here on the first
check, was specific to `care.store`'s `case` entity colliding with
`clojure.core/case`, ADR-2607081500 Decision 5).

### Decision 6: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:learner/intake` (no
capital risk). `:studyplan/verify` and `:dropout-risk/screen` are
never auto-eligible at any phase (matching every sibling's screening-
op posture), and `:actuation/finalize-support-plan`/`:actuation/
contact-guardian` are permanently excluded from every phase's `:auto`
set -- a structural fact, not a rollout milestone, enforced by BOTH
`learning.phase` and `learning.governor`'s `high-stakes` set
independently.

### Decision 7: no bespoke domain capability lib

This vertical's learner records are practice-specific rather than a
shared cross-operator data contract, so `learning.*` runs on the
generic robotics/identity/forms/dmn/bpmn/audit-ledger stack only --
the same posture `9412`/`8720`/`8521`/`3030`/`3830`/`7020`/`9420`/
`9491`/`3512`/`8810`/`8691` and others without a bespoke capability
lib already establish.

### Decision 8: mock + LLM advisor pair

`learning.learningadvisor` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
finalizing a support plan or auto-contacting a guardian).

### Decision 9: blueprint.edn field-sync fixes

Two stale-scaffold inconsistencies in `blueprint.edn`, discovered
during the standard "survey blueprint scaffold" step before writing
any code, were fixed as part of this promotion (the same class of fix
`card.6619`'s, `water.3600`'s, `telecom.6190`'s, `aerospace.3030`'s,
`fab.2610`'s, `energy.3512`'s, `care.8810`'s and `navigator.8691`'s
own ADR-0001s document):

1. `:itonami.blueprint/id` was the stale pre-rename value
   `"cloud-itonami-8569"` (missing `isic-`), while the repo folder,
   README title and this actor's own `:business-id` already use the
   corrected `cloud-itonami-isic-8569`. Fixed to match.
2. `:itonami.blueprint/required-technologies`/`:optional-technologies`
   were missing entirely despite the `kotoba-lang/industry` registry's
   own entry for `"8569"` already stating `[:robotics :identity :forms
   :dmn :bpmn :audit-ledger]` / `[:optimization]`. Fixed to match the
   registry exactly.

## Alternatives considered

- **A single "learner-safety" check merging tutor-ratio and dropout-
  risk concerns.** Rejected: tutor-load ratio is a ground-truth
  numeric recompute needing no proposal inspection; dropout-risk
  status is an unconditionally-evaluated flag that must also HARD-hold
  the screening op itself on its own finding -- merging them would
  lose the screening op's self-hold property, the same reasoning
  `care`'s and `navigator`'s ADR-0001s document for their own
  analogous ground-truth/unconditional-flag distinctions.
- **Naming the new screening concept "safeguarding" (echoing this
  blueprint's own operator-guide mention of "escalation path for
  safeguarding concerns").** Rejected: this fleet already has two
  distinct "safeguarding"-shaped concepts (`congregation`'s matter-
  level allegation, `care`'s check-in-surfaced signal); naming a third,
  academically-distinct concept ("dropout risk") with the same word
  would blur three genuinely different real-world concerns under one
  label. Naming it directly after the blueprint's own `:dropout-
  prevention` social-impact tag keeps the concept both distinct and
  well-grounded.
- **Modeling scholarship/support-service matching as a third
  actuation.** Rejected for this R0: the blueprint's Offer names it as
  a matching/recommendation function, not clearly a standalone real-
  world commitment act on the same footing as finalizing a plan or
  contacting a guardian -- deferred to a follow-up op if this vertical
  is extended.

## Consequences

- Fifty-second actor in this fleet (51 implemented before this build).
- Confirms the ratio-based sufficiency check family generalizes to a
  fifth instance, genuinely distinct domain (tutor-load capacity).
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (dropout-risk), grep-verified absent from every prior
  sibling before the claim was finalized.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/learning/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- Two pre-existing `blueprint.edn` inconsistencies (stale ID, missing
  required/optional-technologies fields) fixed as in-scope minor
  consistency work.
