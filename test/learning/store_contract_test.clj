(ns learning.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [learning.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Yui" (:learner-name (store/learner s "learner-1"))))
      (is (= "JPN" (:jurisdiction (store/learner s "learner-1"))))
      (is (= 8 (:cohort-learner-count (store/learner s "learner-1"))))
      (is (= 1 (:cohort-tutor-count (store/learner s "learner-1"))))
      (is (false? (:dropout-risk-unresolved? (store/learner s "learner-1"))))
      (is (= 15 (:cohort-learner-count (store/learner s "learner-3"))))
      (is (true? (:dropout-risk-unresolved? (store/learner s "learner-4"))))
      (is (false? (:support-plan-finalized? (store/learner s "learner-1"))))
      (is (false? (:guardian-contacted? (store/learner s "learner-1"))))
      (is (= ["learner-1" "learner-2" "learner-3" "learner-4"]
             (mapv :id (store/all-learners s))))
      (is (nil? (store/dropout-risk-screen-of s "learner-1")))
      (is (nil? (store/studyplan-of s "learner-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/support-plan-history s)))
      (is (= [] (store/guardian-contact-history s)))
      (is (zero? (store/next-plan-sequence s "JPN")))
      (is (zero? (store/next-contact-sequence s "JPN")))
      (is (false? (store/learner-already-plan-finalized? s "learner-1")))
      (is (false? (store/learner-already-guardian-contacted? s "learner-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :learner/upsert
                                 :value {:id "learner-1" :learner-name "Sato Yui"}})
        (is (= "Sato Yui" (:learner-name (store/learner s "learner-1"))))
        (is (= 1 (:cohort-tutor-count (store/learner s "learner-1"))) "unrelated field preserved"))
      (testing "studyplan / dropout-risk-screen payloads commit and read back"
        (store/commit-record! s {:effect :studyplan/set :path ["learner-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/studyplan-of s "learner-1")))
        (store/commit-record! s {:effect :dropout-risk-screen/set :path ["learner-1"]
                                 :payload {:learner-id "learner-1" :verdict :resolved}})
        (is (= {:learner-id "learner-1" :verdict :resolved} (store/dropout-risk-screen-of s "learner-1"))))
      (testing "support plan drafts a record and advances the sequence"
        (store/commit-record! s {:effect :learner/mark-plan-finalized :path ["learner-1"]})
        (is (= "JPN-PLN-000000" (get (first (store/support-plan-history s)) "record_id")))
        (is (= "support-plan-draft" (get (first (store/support-plan-history s)) "kind")))
        (is (true? (:support-plan-finalized? (store/learner s "learner-1"))))
        (is (= 1 (count (store/support-plan-history s))))
        (is (= 1 (store/next-plan-sequence s "JPN")))
        (is (true? (store/learner-already-plan-finalized? s "learner-1")))
        (is (false? (store/learner-already-plan-finalized? s "learner-2"))))
      (testing "guardian contact drafts a record and advances the sequence"
        (store/commit-record! s {:effect :learner/mark-guardian-contacted :path ["learner-1"]})
        (is (= "JPN-GDN-000000" (get (first (store/guardian-contact-history s)) "record_id")))
        (is (= "guardian-contact-draft" (get (first (store/guardian-contact-history s)) "kind")))
        (is (true? (:guardian-contacted? (store/learner s "learner-1"))))
        (is (= 1 (count (store/guardian-contact-history s))))
        (is (= 1 (store/next-contact-sequence s "JPN")))
        (is (true? (store/learner-already-guardian-contacted? s "learner-1")))
        (is (false? (store/learner-already-guardian-contacted? s "learner-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/learner s "nope")))
    (is (= [] (store/all-learners s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/support-plan-history s)))
    (is (= [] (store/guardian-contact-history s)))
    (is (zero? (store/next-plan-sequence s "JPN")))
    (is (zero? (store/next-contact-sequence s "JPN")))
    (store/with-learners s {"x" {:id "x" :learner-name "n"
                               :cohort-learner-count 8 :cohort-tutor-count 1
                               :dropout-risk-unresolved? false
                               :support-plan-finalized? false :guardian-contacted? false
                               :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:learner-name (store/learner s "x"))))))
