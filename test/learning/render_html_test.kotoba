(ns learning.render-html-test
  "Contract for the build-time operator-console generator.

  The point of these tests is that the console is EVIDENCE. A page that
  renders successfully but shows no governor hold, or that shows rows
  which trace to nothing in the store, proves nothing -- so the
  properties that make it evidence are asserted here rather than left
  to a reviewer's eye."
  (:require [clojure.test :refer [deftest testing is]]
            [kotoba.lang.text :as str]
            [learning.render-html :as rh]
            [learning.store :as store]))

(defn- holds [db]
  (filterv #(= :governor-hold (:t %)) (store/ledger db)))

(deftest demo-drives-the-real-actor
  (let [{:keys [db runs]} (rh/run-demo!)]
    (testing "the scenario actually exercised the actor"
      (is (= 11 (count runs)))
      (is (= 11 (count (store/ledger db)))))

    (testing "every request reached a terminal disposition"
      (is (every? #(contains? #{:commit :hold} (:disposition (:state %))) runs)))))

(deftest console-shows-real-hard-holds
  (let [{:keys [db]} (rh/run-demo!)
        hs (holds db)
        rules (set (mapcat :basis hs))]
    (testing "the run produces HARD holds -- the invariant -main enforces"
      (is (seq hs) "a console with no hold is not evidence of a governor")
      (is (= 5 (count hs))))

    (testing "five DISTINCT governor rules fire, not one rule five times"
      (is (= #{:no-spec-basis
               :learner-to-tutor-ratio-exceeds-maximum
               :dropout-risk-unresolved
               :already-plan-finalized
               :already-guardian-contacted}
             rules)))

    (testing "every hold carries a violation detail a human can act on"
      (is (every? (fn [h] (every? #(and (:rule %) (not (str/blank? (str (:detail %)))))
                                  (:violations h)))
                  hs)))

    (testing "no hold was ever escalated to a human"
      (is (every? #(= :hold (:disposition %)) hs)))))

(deftest actuations-committed-only-after-approval
  (let [{:keys [db runs]} (rh/run-demo!)
        actuation-commits (filter #(and (contains? #{:actuation/finalize-support-plan
                                                     :actuation/contact-guardian}
                                                   (:op (:request %)))
                                        (= :commit (:disposition (:state %))))
                                  runs)]
    (testing "both actuations committed exactly once"
      (is (= 1 (count (store/support-plan-history db))))
      (is (= 1 (count (store/guardian-contact-history db)))))

    (testing "no actuation ever auto-committed -- each carries an approval-granted fact"
      (is (seq actuation-commits))
      (is (every? (fn [r] (some #(= :approval-granted (:t %)) (:audit (:state r))))
                  actuation-commits)))))

(deftest render-is-deterministic-and-traceable
  (let [result (rh/run-demo!)
        db (:db result)
        html-1 (rh/render result)
        html-2 (rh/render result)]
    (testing "rendering is a pure function of the run"
      (is (= html-1 html-2) "no clock, no randomness in the page body"))

    (testing "two independent runs of the same seed render identically"
      (is (= html-1 (rh/render (rh/run-demo!)))))

    (testing "every learner in the store appears on the page"
      (is (every? #(str/includes? html-1 (:id %)) (store/all-learners db))))

    (testing "every drafted record number appears on the page"
      (is (every? #(str/includes? html-1 (get % "record_id"))
                  (concat (store/support-plan-history db)
                          (store/guardian-contact-history db)))))

    (testing "the page carries no unrendered placeholder"
      (is (not (str/includes? html-1 "%s")))
      (is (not (str/includes? html-1 "TODO")))
      (is (not (str/includes? html-1 "clojure.lang."))))))
