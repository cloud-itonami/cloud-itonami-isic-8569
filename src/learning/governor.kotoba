(ns learning.governor
  "Learner Safety Governor -- the independent compliance layer that
  earns the LearningOps-LLM the right to commit. The LLM has no notion
  of educational-support/student-data-privacy regulatory law, whether
  a learner's own assigned cohort's tutor-load ratio actually stays
  within its own recorded maximum, whether a dropout risk against a
  learner has actually stayed unresolved, or when an act stops being a
  draft and becomes a real-world support-plan finalization or guardian
  contact, so this MUST be a separate system able to *reject* a
  proposal and fall back to HOLD -- the learning analog of `cloud-
  itonami-isic-6512`'s CasualtyGovernor.

  Six checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them (you don't get to approve your way past a
  fabricated jurisdiction spec-basis, incomplete evidence, an
  overloaded tutor cohort, an unresolved dropout risk, or a double
  plan-finalization/guardian-contact). The confidence/actuation gate
  is SOFT: it asks a human to look (low confidence / actuation), and
  the human may approve -- but see `learning.phase`: for `:stake
  :actuation/finalize-support-plan`/`:actuation/contact-guardian` (a
  real support-plan act or a real guardian-contact act) NO phase ever
  allows auto-commit either. Two independent layers agree that
  actuation is always a human call.

    1. Spec-basis                  -- did the study-plan proposal cite
                                       an OFFICIAL source (`learning.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/finalize-
                                       support-plan`/`:actuation/
                                       contact-guardian`, has the
                                       learner actually been assessed
                                       with a full consent-record/
                                       guardian-consent-record/
                                       learner-data-purpose-
                                       limitation-record/support-plan-
                                       record evidence checklist on
                                       file?
    3. Learner-to-tutor ratio
       exceeds maximum                -- for `:actuation/finalize-
                                       support-plan`, INDEPENDENTLY
                                       recompute whether the learner's
                                       own assigned cohort's learner
                                       count divided by its own tutor
                                       count exceeds the fixed policy
                                       maximum (`learning.registry/
                                       learner-to-tutor-ratio-exceeds-
                                       maximum?`) -- needs no proposal
                                       inspection at all. The FIFTH
                                       instance of this fleet's ratio-
                                       based sufficiency check family
                                       (`leasing.governor/collateral-
                                       coverage-ratio-insufficient-
                                       violations`/`behavioral.
                                       governor/supervision-ratio-
                                       insufficient-violations`/
                                       `union.governor/strike-vote-
                                       share-insufficient-violations`/
                                       `fab.governor/yield-rate-
                                       insufficient-violations`
                                       established the first four).
    4. Dropout risk unresolved     -- reported by THIS proposal itself
                                       (a `:dropout-risk/screen` that
                                       just found one), or already on
                                       file for the learner (`:dropout-
                                       risk/screen`/`:actuation/
                                       finalize-support-plan`).
                                       Evaluated UNCONDITIONALLY (not
                                       scoped to a specific op), the
                                       SAME discipline `casualty.
                                       governor/sanctions-violations`/
                                       ...(thirty-five prior siblings,
                                       most recently `navigator.
                                       governor/urgent-health-risk-
                                       unresolved-violations`)...
                                       established -- the THIRTY-
                                       SIXTH distinct application of
                                       this exact discipline overall,
                                       and a genuinely NEW concept
                                       (grep-verified absent from
                                       every prior sibling's check
                                       names before this claim was
                                       finalized), grounded directly in
                                       this blueprint's own `:social-
                                       impact` tag `:dropout-
                                       prevention`. Exercised in
                                       tests/demo via `:dropout-risk/
                                       screen` DIRECTLY, not via the
                                       actuation op against an
                                       unscreened learner -- see this
                                       ns's own test suite.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       finalize-support-plan`/
                                       `:actuation/contact-guardian`
                                       (REAL learning-support acts) ->
                                       escalate.

  Two more guards, double-plan/double-contact prevention, are enforced
  but NOT listed as numbered HARD checks above because they need no
  upstream comparison at all -- `already-plan-finalized-violations`/
  `already-guardian-contacted-violations` refuse to finalize a support
  plan/contact a guardian for the SAME learner twice, off dedicated
  `:support-plan-finalized?`/`:guardian-contacted?` facts (never a
  `:status` value) -- the SAME 'check a dedicated boolean, not status'
  discipline every prior sibling governor's guards establish, informed
  by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320)."
  (:require [learning.facts :as facts]
            [learning.registry :as registry]
            [learning.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Finalizing a real support plan and contacting a real guardian are
  the two real-world actuation events this actor performs -- a two-
  member set, matching every prior dual-actuation sibling's shape.
  Both are POSITIVE actuations (finalizing/issuing a real record),
  matching this fleet's majority actuation shape (3600/6190 remain the
  only negative-actuation exceptions)."
  #{:actuation/finalize-support-plan :actuation/contact-guardian})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:studyplan/verify` (or actuation) proposal with no spec-basis
  citation is a HARD violation -- never invent a jurisdiction's
  educational-support/student-data-privacy requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:studyplan/verify :actuation/finalize-support-plan :actuation/contact-guardian} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は学習支援運営基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/finalize-support-plan`/`:actuation/contact-
  guardian`, the jurisdiction's required consent-record/guardian-
  consent-record/learner-data-purpose-limitation-record/support-plan-
  record evidence must actually be satisfied -- do not trust the
  advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:actuation/finalize-support-plan :actuation/contact-guardian} op)
    (let [l (store/learner st subject)
          plan (store/studyplan-of st subject)]
      (when-not (and plan
                     (facts/required-evidence-satisfied?
                      (:jurisdiction l) (:checklist plan)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(同意記録/保護者同意記録/学習者情報目的制限記録/支援計画記録等)が充足していない状態での提案"}]))))

(defn- learner-to-tutor-ratio-exceeds-maximum-violations
  "For `:actuation/finalize-support-plan`, INDEPENDENTLY recompute
  whether the learner's own assigned cohort's learner count divided by
  its own tutor count exceeds the fixed policy maximum via `learning.
  registry/learner-to-tutor-ratio-exceeds-maximum?` -- needs no
  proposal inspection at all, since its inputs are permanent ground-
  truth fields already on the learner."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-support-plan)
    (let [l (store/learner st subject)]
      (cond
        ;; The entity EXISTS but the figure it needs is missing or
        ;; non-numeric, so the limit cannot be evaluated -- which is not
        ;; the same as being within it. A missing entity is a different
        ;; violation that another gate owns, so it is excluded here.
        (and l (not (registry/learner-to-tutor-ratio-exceeds-maximum-checkable? l)))
        [{:rule :learner-to-tutor-ratio-exceeds-maximum
          :detail "上限判定に必要な値が記録されていない -- 限度内と断定できないため進めない"}]

        (registry/learner-to-tutor-ratio-exceeds-maximum? l)
        [{:rule :learner-to-tutor-ratio-exceeds-maximum
          :detail (str subject " の担当コホート学習者数(" (:cohort-learner-count l)
                      ")/チューター数(" (:cohort-tutor-count l) ")が上限を超過")}]))))

(defn- dropout-risk-unresolved-violations
  "An unresolved dropout risk -- reported by THIS proposal (e.g. a
  `:dropout-risk/screen` that itself just found one), or already on
  file in the store for the learner (`:dropout-risk/screen`/
  `:actuation/finalize-support-plan`) -- is a HARD, un-overridable
  hold. Evaluated UNCONDITIONALLY (not scoped to a specific op) so the
  screening op itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        learner-id (when (contains? #{:dropout-risk/screen :actuation/finalize-support-plan} op) subject)
        hit-on-file? (and learner-id (= :unresolved (:verdict (store/dropout-risk-screen-of st learner-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :dropout-risk-unresolved
        :detail "未解決の中退リスクがある状態での支援計画確定提案は進められない"}])))

(defn- already-plan-finalized-violations
  "For `:actuation/finalize-support-plan`, refuses to finalize a
  support plan for the SAME learner twice, off a dedicated `:support-
  plan-finalized?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-support-plan)
    (when (store/learner-already-plan-finalized? st subject)
      [{:rule :already-plan-finalized
        :detail (str subject " は既に支援計画確定済み")}])))

(defn- already-guardian-contacted-violations
  "For `:actuation/contact-guardian`, refuses to contact the SAME
  learner's guardian twice, off a dedicated `:guardian-contacted?`
  fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/contact-guardian)
    (when (store/learner-already-guardian-contacted? st subject)
      [{:rule :already-guardian-contacted
        :detail (str subject " は既に保護者連絡済み")}])))

(defn check
  "Censors a LearningOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (learner-to-tutor-ratio-exceeds-maximum-violations request st)
                           (dropout-risk-unresolved-violations request proposal st)
                           (already-plan-finalized-violations request st)
                           (already-guardian-contacted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
