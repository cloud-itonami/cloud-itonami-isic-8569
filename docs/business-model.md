# Business Model: Community Learning Support

## Classification

- Repository: `cloud-itonami-isic-8569`
- ISIC Rev.5: `8569`
- Activity: educational support activities
- Social impact: learning access, dropout prevention, family support

## Customer

- schools
- after-school programs
- local governments
- NPOs
- tutoring operators
- scholarship programs

## Offer

- learner intake and consent workflow
- study-plan generation with human review
- attendance and progress follow-up
- scholarship and support-service matching
- guardian communication queue
- outcome and equity reporting

## Revenue

- program setup fee
- per-learner monthly support
- school/NPO support contract
- grant-reporting package
- operator training and certification

## Trust Controls

- learner data is purpose-limited
- guardian/school consent is required
- high-stakes recommendations require human approval
- equity and access metrics are audited
- a fabricated jurisdiction citation, incomplete evidence, an overloaded
  tutor cohort, or an unresolved dropout risk -- each forces a hold, not
  an override
- support-plan finalization and guardian contact are logged and escalated,
  and neither can be finalized twice for the same learner: a double-
  finalization or double-contact attempt is held off this actor's own
  learner facts alone, with no upstream comparison needed

## Learner Safety Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:learner-safety-
governor` -- this is not a generic "review step," it is the one gate
every proposed action in this business must pass before a support plan
is finalized or a guardian is contacted. The governor sits between the
Learning Advisor and execution, per the README's Core Contract:

```text
Learning Advisor -> Learner Safety Governor -> plan, hold, or approve
```

**Approves**: routine learning-support actions proposed against a
learner that already has a consented intake record on file, a cohort
within its own tutor-load ratio, and no unresolved dropout risk. These
proceed straight to the support-plan / progress ledger.

**Rejects or escalates**: the governor refuses to let the advisor
finalize a support plan or contact a guardian on its own authority when
any of the following hold -- a fabricated jurisdiction spec-basis;
incomplete evidence; a cohort's learner-to-tutor ratio exceeding its own
maximum; an unresolved dropout risk. A clean plan/contact proposal
still always routes to a human -- neither `:actuation/finalize-support-
plan` nor `:actuation/contact-guardian` is ever auto-committed, at any
rollout phase.
