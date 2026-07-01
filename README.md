# cloud-itonami-8569

Open Business Blueprint for **ISIC Rev.5 8569**: educational support
activities.

This repository designs a forkable OSS business for learning support, tutoring
operations, attendance follow-up, scholarship guidance and school-community
case management.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a learning-support robot performs content delivery, assessment proctoring and assistive interaction with learners under an actor that proposes
actions and an independent **Learner Safety Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near children or vulnerable learners) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
learner record + goals + consent + school context
        |
        v
Learning Advisor -> Child/Learner Safety Governor -> plan, hold, or approve
        |
        v
support plan + progress ledger
```

The advisor can draft support plans, but cannot expose learner data, make
high-stakes placement decisions or contact guardians without policy approval.

## Runbook

- Start with consented, minimal learner records.
- Add study plans and progress tracking.
- Add human-reviewed recommendations.
- Add scholarship and service referrals.

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

Code and implementation templates are AGPL-3.0-or-later.
