(ns learning.registry
  "Pure-function support-plan-finalization + guardian-contact record
  construction -- an append-only community-learning-support book-of-
  record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a support-plan or guardian-
  contact reference number -- every learning-support provider/
  jurisdiction assigns its own reference format. This namespace does
  NOT invent one; it builds a jurisdiction-scoped sequence number and
  validates the record's required fields, the same honest, non-
  fabricating discipline `learning.facts` uses.

  `learner-to-tutor-ratio-exceeds-maximum?` is the FIFTH instance of
  this fleet's ratio-based sufficiency check family (`leasing.
  registry/collateral-coverage-ratio-insufficient?` established the
  first, MINIMUM-floor direction; `behavioral.registry/supervision-
  ratio-insufficient?` the second, MAXIMUM-ceiling direction; `union.
  registry/strike-vote-share-insufficient?` the third, MINIMUM-floor
  direction again; `fab.registry/yield-rate-insufficient?` the fourth,
  MINIMUM-floor direction again), applying the SAME quotient-
  comparison shape -- MAXIMUM-ceiling direction, like `behavioral`'s --
  to a learner's own assigned cohort's recorded learner count divided
  by its own recorded tutor count, which must NOT exceed a maximum
  permitted tutor-load ratio before a support plan can be finalized --
  a direct, natural mapping onto real tutoring-quality-of-service
  practice (a support plan finalized for a learner in an already-
  overloaded tutor cohort is exactly the failure mode a community
  learning-support operator must not let an advisor wave through).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real learning-support system. It builds the RECORD a
  learning-support operator would keep, not the act of finalizing the
  support plan or contacting the guardian itself (that is `learning.
  operation`'s `:actuation/finalize-support-plan`/`:actuation/contact-
  guardian`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  learning-support operator's own act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(def maximum-tutor-load-ratio
  "The fixed policy ceiling this R0 checks against -- no more than 12
  learners per tutor in an active cohort. A starting, honestly-
  documented simplification (see ns docstring), not a from-scratch
  survey of every jurisdiction's own tutoring-quality-of-service
  staffing standard, the same honest-simplification discipline
  `behavioral.registry/maximum-supervision-ratio` uses."
  12)

(defn learner-to-tutor-ratio-exceeds-maximum?
  "Does `learner`'s own assigned cohort's `:cohort-learner-count`
  divided by its own `:cohort-tutor-count` exceed `maximum-tutor-load-
  ratio`? A pure ground-truth check against the learner's own
  permanent fields -- no upstream comparison needed. The FIFTH
  instance of this fleet's ratio-based sufficiency check family (see
  ns docstring), applying the MAXIMUM direction like `behavioral.
  registry/supervision-ratio-insufficient?`."
  [{:keys [cohort-learner-count cohort-tutor-count]}]
  (and (number? cohort-learner-count) (number? cohort-tutor-count) (pos? cohort-tutor-count)
       (> (/ cohort-learner-count cohort-tutor-count) maximum-tutor-load-ratio)))

(defn learner-to-tutor-ratio-exceeds-maximum-checkable?
  "Are the figures `learner-to-tutor-ratio-exceeds-maximum?` needs actually recorded?

  That predicate answers only `over` / `not over`, and its
  `(and (number? ...) ...)` guard made an un-recorded figure fall
  through as `not over`. A cohort with no recorded head counts read as within the ratio. Callers must ask this first:
  un-checkable is not within limits."
  [{:keys [cohort-learner-count cohort-tutor-count]}]
  (boolean (and (number? cohort-learner-count) (number? cohort-tutor-count))))

(defn register-support-plan
  "Validate + construct the SUPPORT-PLAN registration DRAFT -- the
  learning-support operator's own act of finalizing a real support
  plan for a learner. Pure function -- does not touch any real
  learning-support system; it builds the RECORD an operator would
  keep. `learning.governor` independently re-verifies the learner's
  own cohort tutor-load ratio and dropout-risk resolution status, and
  blocks a double-finalization for the same learner, before this is
  ever allowed to commit."
  [learner-id jurisdiction sequence]
  (when-not (and learner-id (not= learner-id ""))
    (throw (ex-info "support-plan: learner_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "support-plan: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "support-plan: sequence must be >= 0" {})))
  (let [plan-number (str (str/upper-case jurisdiction) "-PLN-" (zero-pad sequence 6))
        record {"record_id" plan-number
                "kind" "support-plan-draft"
                "learner_id" learner-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "plan_number" plan-number
     "certificate" (unsigned-certificate "SupportPlan" plan-number plan-number)}))

(defn register-guardian-contact
  "Validate + construct the GUARDIAN-CONTACT registration DRAFT -- the
  learning-support operator's own act of contacting a real learner's
  guardian. Pure function -- does not touch any real learning-support
  system; it builds the RECORD an operator would keep. `learning.
  governor` independently re-verifies the learner's own consent/
  purpose-limitation evidence and blocks a double-contact for the same
  learner, before this is ever allowed to commit."
  [learner-id jurisdiction sequence]
  (when-not (and learner-id (not= learner-id ""))
    (throw (ex-info "guardian-contact: learner_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "guardian-contact: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "guardian-contact: sequence must be >= 0" {})))
  (let [contact-number (str (str/upper-case jurisdiction) "-GDN-" (zero-pad sequence 6))
        record {"record_id" contact-number
                "kind" "guardian-contact-draft"
                "learner_id" learner-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "contact_number" contact-number
     "certificate" (unsigned-certificate "GuardianContact" contact-number contact-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
