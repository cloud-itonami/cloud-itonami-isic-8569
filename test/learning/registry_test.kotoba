(ns learning.registry-test
  (:require [clojure.test :refer [deftest is]]
            [learning.registry :as r]))

;; ----------------------------- learner-to-tutor-ratio-exceeds-maximum? -----------------------------

(deftest not-exceeded-when-within-max-ratio
  (is (not (r/learner-to-tutor-ratio-exceeds-maximum? {:cohort-learner-count 8 :cohort-tutor-count 1})))
  (is (not (r/learner-to-tutor-ratio-exceeds-maximum? {:cohort-learner-count 12 :cohort-tutor-count 1}))))

(deftest exceeded-when-over-max-ratio
  (is (r/learner-to-tutor-ratio-exceeds-maximum? {:cohort-learner-count 15 :cohort-tutor-count 1}))
  (is (r/learner-to-tutor-ratio-exceeds-maximum? {:cohort-learner-count 13 :cohort-tutor-count 1})))

(deftest exceeded-is-false-on-missing-or-zero-fields
  (is (not (r/learner-to-tutor-ratio-exceeds-maximum? {})))
  (is (not (r/learner-to-tutor-ratio-exceeds-maximum? {:cohort-learner-count 15})))
  (is (not (r/learner-to-tutor-ratio-exceeds-maximum? {:cohort-learner-count 15 :cohort-tutor-count 0}))
      "zero tutors -> guarded, not a division-by-zero crash"))

;; ----------------------------- register-support-plan -----------------------------

(deftest plan-is-a-draft-not-a-real-plan
  (let [result (r/register-support-plan "learner-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest plan-assigns-plan-number
  (let [result (r/register-support-plan "learner-1" "JPN" 7)]
    (is (= (get result "plan_number") "JPN-PLN-000007"))
    (is (= (get-in result ["record" "learner_id"]) "learner-1"))
    (is (= (get-in result ["record" "kind"]) "support-plan-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest plan-validation-rules
  (is (thrown? Exception (r/register-support-plan "" "JPN" 0)))
  (is (thrown? Exception (r/register-support-plan "learner-1" "" 0)))
  (is (thrown? Exception (r/register-support-plan "learner-1" "JPN" -1))))

;; ----------------------------- register-guardian-contact -----------------------------

(deftest contact-is-a-draft-not-a-real-contact
  (let [result (r/register-guardian-contact "learner-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest contact-assigns-contact-number
  (let [result (r/register-guardian-contact "learner-1" "JPN" 3)]
    (is (= (get result "contact_number") "JPN-GDN-000003"))
    (is (= (get-in result ["record" "learner_id"]) "learner-1"))
    (is (= (get-in result ["record" "kind"]) "guardian-contact-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest contact-validation-rules
  (is (thrown? Exception (r/register-guardian-contact "" "JPN" 0)))
  (is (thrown? Exception (r/register-guardian-contact "learner-1" "" 0)))
  (is (thrown? Exception (r/register-guardian-contact "learner-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-support-plan "learner-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-support-plan "learner-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-PLN-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-PLN-000001" (get-in hist2 [1 "record_id"])))))
