(ns learning.governor-contract-test
  "The governor contract as executable tests -- the learning analog of
  `cloud-itonami-isic-6512`'s `casualty.governor-contract-test`. The
  single invariant under test:

    LearningOps-LLM never finalizes a support plan or contacts a
    guardian the Learner Safety Governor would reject, `:actuation/
    finalize-support-plan`/`:actuation/contact-guardian` NEVER auto-
    commit at any phase, `:learner/intake` (no direct capital risk)
    MAY auto-commit when clean, and every decision (commit OR hold)
    leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [learning.store :as store]
            [learning.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :learning-coordinator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a study-plan
  assessment on file. Uses distinct thread-ids per call site by
  suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :studyplan/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(defn- screen!
  "Walks `subject` through dropout-risk screening -> approve, leaving
  a screening on file. Only safe to call for a learner whose risk
  status has already resolved -- an unresolved risk HARD-holds the
  screen itself (see `dropout-risk-is-held-and-unoverridable`)."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-screen") {:op :dropout-risk/screen :subject subject} operator)
  (approve! actor (str tid-prefix "-screen")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :learner/intake :subject "learner-1"
                   :patch {:id "learner-1" :learner-name "Sato Yui"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sato Yui" (:learner-name (store/learner db "learner-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest studyplan-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :studyplan/verify :subject "learner-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/studyplan-of db "learner-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a studyplan/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :studyplan/verify :subject "learner-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/studyplan-of db "learner-1")) "no study-plan assessment written"))))

(deftest finalize-support-plan-without-studyplan-is-held
  (testing "actuation/finalize-support-plan before any study-plan verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/finalize-support-plan :subject "learner-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest learner-to-tutor-ratio-exceeds-maximum-is-held
  (testing "a learner whose own cohort's learner-to-tutor ratio exceeds its own maximum -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "learner-3")
          res (exec-op actor "t5" {:op :actuation/finalize-support-plan :subject "learner-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:learner-to-tutor-ratio-exceeds-maximum} (-> (store/ledger db) last :basis)))
      (is (empty? (store/support-plan-history db))))))

(deftest dropout-risk-is-held-and-unoverridable
  (testing "an unresolved dropout risk on a learner -> HOLD, and never reaches request-approval -- exercised via :dropout-risk/screen DIRECTLY, not via the actuation op against an unscreened learner (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's, recovery's, consulting's, union's, congregation's, fab's, energy's, care's and navigator's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :dropout-risk/screen :subject "learner-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:dropout-risk-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/dropout-risk-screen-of db "learner-4")) "no clearance written"))))

(deftest finalize-support-plan-always-escalates-then-human-decides
  (testing "a clean, fully-assessed learner still ALWAYS interrupts for human approval -- actuation/finalize-support-plan is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "learner-1")
          r1 (exec-op actor "t7" {:op :actuation/finalize-support-plan :subject "learner-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, support-plan record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:support-plan-finalized? (store/learner db "learner-1"))))
          (is (= 1 (count (store/support-plan-history db))) "one draft support-plan record"))))))

(deftest contact-guardian-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, risk-resolved learner still ALWAYS interrupts for human approval -- actuation/contact-guardian is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "learner-1")
          _ (screen! actor "t8pre2" "learner-1")
          r1 (exec-op actor "t8" {:op :actuation/contact-guardian :subject "learner-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, guardian-contact record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:guardian-contacted? (store/learner db "learner-1"))))
          (is (= 1 (count (store/guardian-contact-history db))) "one draft guardian-contact record"))))))

(deftest finalize-support-plan-double-finalization-is-held
  (testing "finalizing the same learner's support plan twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t9pre" "learner-1")
          _ (exec-op actor "t9a" {:op :actuation/finalize-support-plan :subject "learner-1"} operator)
          _ (approve! actor "t9a")
          res (exec-op actor "t9" {:op :actuation/finalize-support-plan :subject "learner-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-plan-finalized} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/support-plan-history db))) "still only the one earlier finalization"))))

(deftest contact-guardian-double-contact-is-held
  (testing "contacting the same learner's guardian twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t10pre" "learner-1")
          _ (screen! actor "t10pre2" "learner-1")
          _ (exec-op actor "t10a" {:op :actuation/contact-guardian :subject "learner-1"} operator)
          _ (approve! actor "t10a")
          res (exec-op actor "t10" {:op :actuation/contact-guardian :subject "learner-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-guardian-contacted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/guardian-contact-history db))) "still only the one earlier contact"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :learner/intake :subject "learner-1"
                          :patch {:id "learner-1" :learner-name "Sato Yui"}} operator)
      (exec-op actor "b" {:op :studyplan/verify :subject "learner-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
