(ns learning.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:actuation/finalize-support-plan`/`:actuation/contact-
  guardian` must NEVER be a member of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [learning.phase :as phase]))

(deftest finalize-support-plan-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real support-plan finalization"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/finalize-support-plan))
          (str "phase " n " must not auto-commit :actuation/finalize-support-plan")))))

(deftest contact-guardian-never-auto-at-any-phase
  (testing "structural invariant: no phase auto-commits a real guardian contact"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/contact-guardian))
          (str "phase " n " must not auto-commit :actuation/contact-guardian")))))

(deftest dropout-risk-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :dropout-risk/screen))
          (str "phase " n " must not auto-commit :dropout-risk/screen")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":learner/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:learner/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :learner/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/finalize-support-plan} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/contact-guardian} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :learner/intake} :commit)))))
