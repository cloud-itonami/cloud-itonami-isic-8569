(ns learning.store
  "SSoT for the learning actor, behind a `Store` protocol so the
  backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/learning/store_contract_test.clj), which is the whole point:
  the actor, the Learner Safety Governor and the audit ledger never
  know which SSoT they run on.

  Like every prior dual-actuation sibling, this actor has TWO
  actuation events (finalizing a support plan, contacting a guardian)
  acting on the SAME entity (a `learner`), each with its OWN history
  collection, sequence counter and dedicated double-actuation-guard
  boolean (`:support-plan-finalized?`/`:guardian-contacted?`, never a
  `:status` value) -- the same discipline every prior sibling
  governor's guards establish, informed by `cloud-itonami-isic-6492`'s
  status-lifecycle bug (ADR-2607071320).

  The ledger stays append-only on every backend: 'which learner was
  screened for an unresolved dropout risk, which support plan was
  finalized, which guardian was contacted, on what jurisdictional
  basis, approved by whom' is always a query over an immutable log --
  the audit trail a family trusting a learning-support operator needs,
  and the evidence an operator needs if a plan or contact decision is
  later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [learning.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (learner [s id])
  (all-learners [s])
  (dropout-risk-screen-of [s learner-id] "committed dropout-risk screening verdict for a learner, or nil")
  (studyplan-of [s learner-id] "committed study-plan evidence assessment, or nil")
  (ledger [s])
  (support-plan-history [s] "the append-only support-plan history (learning.registry drafts)")
  (guardian-contact-history [s] "the append-only guardian-contact history (learning.registry drafts)")
  (next-plan-sequence [s jurisdiction] "next plan-number sequence for a jurisdiction")
  (next-contact-sequence [s jurisdiction] "next contact-number sequence for a jurisdiction")
  (learner-already-plan-finalized? [s learner-id] "has this learner's support plan already been finalized?")
  (learner-already-guardian-contacted? [s learner-id] "has this learner's guardian already been contacted?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-learners [s learners] "replace/seed the learner directory (map id->learner)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained learner set covering both actuation
  lifecycles (finalizing a support plan, contacting a guardian) so the
  actor + tests run offline."
  []
  {:learners
   {"learner-1" {:id "learner-1" :learner-name "Sato Yui"
                :cohort-learner-count 8 :cohort-tutor-count 1
                :dropout-risk-unresolved? false
                :support-plan-finalized? false :guardian-contacted? false
                :jurisdiction "JPN" :status :intake}
    "learner-2" {:id "learner-2" :learner-name "Atlantis Doe"
                :cohort-learner-count 8 :cohort-tutor-count 1
                :dropout-risk-unresolved? false
                :support-plan-finalized? false :guardian-contacted? false
                :jurisdiction "ATL" :status :intake}
    "learner-3" {:id "learner-3" :learner-name "鈴木翔太"
                :cohort-learner-count 15 :cohort-tutor-count 1
                :dropout-risk-unresolved? false
                :support-plan-finalized? false :guardian-contacted? false
                :jurisdiction "JPN" :status :intake}
    "learner-4" {:id "learner-4" :learner-name "田中美咲"
                :cohort-learner-count 8 :cohort-tutor-count 1
                :dropout-risk-unresolved? true
                :support-plan-finalized? false :guardian-contacted? false
                :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- finalize-support-plan!
  "Backend-agnostic `:learner/mark-plan-finalized` -- looks up the
  learner via the protocol and drafts the support-plan record, and
  returns {:result .. :learner-patch ..} for the caller to persist."
  [s learner-id]
  (let [l (learner s learner-id)
        seq-n (next-plan-sequence s (:jurisdiction l))
        result (registry/register-support-plan learner-id (:jurisdiction l) seq-n)]
    {:result result
     :learner-patch {:support-plan-finalized? true
                    :plan-number (get result "plan_number")}}))

(defn- contact-guardian!
  "Backend-agnostic `:learner/mark-guardian-contacted` -- looks up the
  learner via the protocol and drafts the guardian-contact record, and
  returns {:result .. :learner-patch ..} for the caller to persist."
  [s learner-id]
  (let [l (learner s learner-id)
        seq-n (next-contact-sequence s (:jurisdiction l))
        result (registry/register-guardian-contact learner-id (:jurisdiction l) seq-n)]
    {:result result
     :learner-patch {:guardian-contacted? true
                    :contact-number (get result "contact_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (learner [_ id] (get-in @a [:learners id]))
  (all-learners [_] (sort-by :id (vals (:learners @a))))
  (dropout-risk-screen-of [_ id] (get-in @a [:dropout-risk-screens id]))
  (studyplan-of [_ learner-id] (get-in @a [:studyplans learner-id]))
  (ledger [_] (:ledger @a))
  (support-plan-history [_] (:support-plans @a))
  (guardian-contact-history [_] (:guardian-contacts @a))
  (next-plan-sequence [_ jurisdiction] (get-in @a [:plan-sequences jurisdiction] 0))
  (next-contact-sequence [_ jurisdiction] (get-in @a [:contact-sequences jurisdiction] 0))
  (learner-already-plan-finalized? [_ learner-id] (boolean (get-in @a [:learners learner-id :support-plan-finalized?])))
  (learner-already-guardian-contacted? [_ learner-id] (boolean (get-in @a [:learners learner-id :guardian-contacted?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :learner/upsert
      (swap! a update-in [:learners (:id value)] merge value)

      :studyplan/set
      (swap! a assoc-in [:studyplans (first path)] payload)

      :dropout-risk-screen/set
      (swap! a assoc-in [:dropout-risk-screens (first path)] payload)

      :learner/mark-plan-finalized
      (let [learner-id (first path)
            {:keys [result learner-patch]} (finalize-support-plan! s learner-id)
            jurisdiction (:jurisdiction (learner s learner-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:plan-sequences jurisdiction] (fnil inc 0))
                       (update-in [:learners learner-id] merge learner-patch)
                       (update :support-plans registry/append result))))
        result)

      :learner/mark-guardian-contacted
      (let [learner-id (first path)
            {:keys [result learner-patch]} (contact-guardian! s learner-id)
            jurisdiction (:jurisdiction (learner s learner-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:contact-sequences jurisdiction] (fnil inc 0))
                       (update-in [:learners learner-id] merge learner-patch)
                       (update :guardian-contacts registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-learners [s learners] (when (seq learners) (swap! a assoc :learners learners)) s))

(defn seed-db
  "A MemStore seeded with the demo learner set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :studyplans {} :dropout-risk-screens {} :ledger [] :plan-sequences {}
                           :support-plans [] :contact-sequences {} :guardian-contacts []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (studyplan/dropout-risk-screen payloads, ledger
  facts, support-plan/guardian-contact records) are stored as EDN
  strings so `langchain.db` doesn't expand them into sub-entities --
  the same convention every sibling actor's store uses."
  {:learner/id                          {:db/unique :db.unique/identity}
   :studyplan/learner-id                {:db/unique :db.unique/identity}
   :dropout-risk-screen/learner-id      {:db/unique :db.unique/identity}
   :ledger/seq                         {:db/unique :db.unique/identity}
   :plan/seq                           {:db/unique :db.unique/identity}
   :guardian-contact/seq               {:db/unique :db.unique/identity}
   :plan-sequence/jurisdiction         {:db/unique :db.unique/identity}
   :contact-sequence/jurisdiction      {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- learner->tx [{:keys [id learner-name cohort-learner-count cohort-tutor-count
                          dropout-risk-unresolved?
                          support-plan-finalized? guardian-contacted?
                          jurisdiction status plan-number contact-number]}]
  (cond-> {:learner/id id}
    learner-name                                (assoc :learner/learner-name learner-name)
    cohort-learner-count                        (assoc :learner/cohort-learner-count cohort-learner-count)
    cohort-tutor-count                          (assoc :learner/cohort-tutor-count cohort-tutor-count)
    (some? dropout-risk-unresolved?)            (assoc :learner/dropout-risk-unresolved? dropout-risk-unresolved?)
    (some? support-plan-finalized?)             (assoc :learner/support-plan-finalized? support-plan-finalized?)
    (some? guardian-contacted?)                 (assoc :learner/guardian-contacted? guardian-contacted?)
    jurisdiction                                 (assoc :learner/jurisdiction jurisdiction)
    status                                       (assoc :learner/status status)
    plan-number                                  (assoc :learner/plan-number plan-number)
    contact-number                               (assoc :learner/contact-number contact-number)))

(def ^:private learner-pull
  [:learner/id :learner/learner-name :learner/cohort-learner-count :learner/cohort-tutor-count
   :learner/dropout-risk-unresolved? :learner/support-plan-finalized? :learner/guardian-contacted?
   :learner/jurisdiction :learner/status :learner/plan-number :learner/contact-number])

(defn- pull->learner [m]
  (when (:learner/id m)
    {:id (:learner/id m) :learner-name (:learner/learner-name m)
     :cohort-learner-count (:learner/cohort-learner-count m)
     :cohort-tutor-count (:learner/cohort-tutor-count m)
     :dropout-risk-unresolved? (boolean (:learner/dropout-risk-unresolved? m))
     :support-plan-finalized? (boolean (:learner/support-plan-finalized? m))
     :guardian-contacted? (boolean (:learner/guardian-contacted? m))
     :jurisdiction (:learner/jurisdiction m) :status (:learner/status m)
     :plan-number (:learner/plan-number m) :contact-number (:learner/contact-number m)}))

(defrecord DatomicStore [conn]
  Store
  (learner [_ id]
    (pull->learner (d/pull (d/db conn) learner-pull [:learner/id id])))
  (all-learners [_]
    (->> (d/q '[:find [?id ...] :where [?e :learner/id ?id]] (d/db conn))
         (map #(pull->learner (d/pull (d/db conn) learner-pull [:learner/id %])))
         (sort-by :id)))
  (dropout-risk-screen-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?lid
                :where [?k :dropout-risk-screen/learner-id ?lid] [?k :dropout-risk-screen/payload ?p]]
              (d/db conn) id)))
  (studyplan-of [_ learner-id]
    (dec* (d/q '[:find ?p . :in $ ?lid
                :where [?a :studyplan/learner-id ?lid] [?a :studyplan/payload ?p]]
              (d/db conn) learner-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (support-plan-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :plan/seq ?s] [?e :plan/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (guardian-contact-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :guardian-contact/seq ?s] [?e :guardian-contact/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-plan-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :plan-sequence/jurisdiction ?j] [?e :plan-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-contact-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :contact-sequence/jurisdiction ?j] [?e :contact-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (learner-already-plan-finalized? [s learner-id]
    (boolean (:support-plan-finalized? (learner s learner-id))))
  (learner-already-guardian-contacted? [s learner-id]
    (boolean (:guardian-contacted? (learner s learner-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :learner/upsert
      (d/transact! conn [(learner->tx value)])

      :studyplan/set
      (d/transact! conn [{:studyplan/learner-id (first path) :studyplan/payload (enc payload)}])

      :dropout-risk-screen/set
      (d/transact! conn [{:dropout-risk-screen/learner-id (first path) :dropout-risk-screen/payload (enc payload)}])

      :learner/mark-plan-finalized
      (let [learner-id (first path)
            {:keys [result learner-patch]} (finalize-support-plan! s learner-id)
            jurisdiction (:jurisdiction (learner s learner-id))
            next-n (inc (next-plan-sequence s jurisdiction))]
        (d/transact! conn
                     [(learner->tx (assoc learner-patch :id learner-id))
                      {:plan-sequence/jurisdiction jurisdiction :plan-sequence/next next-n}
                      {:plan/seq (count (support-plan-history s)) :plan/record (enc (get result "record"))}])
        result)

      :learner/mark-guardian-contacted
      (let [learner-id (first path)
            {:keys [result learner-patch]} (contact-guardian! s learner-id)
            jurisdiction (:jurisdiction (learner s learner-id))
            next-n (inc (next-contact-sequence s jurisdiction))]
        (d/transact! conn
                     [(learner->tx (assoc learner-patch :id learner-id))
                      {:contact-sequence/jurisdiction jurisdiction :contact-sequence/next next-n}
                      {:guardian-contact/seq (count (guardian-contact-history s)) :guardian-contact/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-learners [s learners]
    (when (seq learners) (d/transact! conn (mapv learner->tx (vals learners)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:learners ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [learners]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-learners s learners))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo learner set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
