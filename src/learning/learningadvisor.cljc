(ns learning.learningadvisor
  "LearningOps-LLM client -- the *contained intelligence node* for the
  learning actor (README: \"Learning Advisor\").

  It normalizes learner-intake, drafts a per-jurisdiction educational-
  support/student-data-privacy evidence checklist, screens learners
  for an unresolved dropout risk, drafts the support-plan-finalization
  action, and drafts the guardian-contact action. CRITICAL: it is a
  smart-but-untrusted advisor. It returns a *proposal* (with a
  rationale + the fields it cited), never a committed record or a real
  support-plan finalization/guardian contact. Every output is censored
  downstream by `learning.governor` before anything touches the SSoT,
  and `:actuation/finalize-support-plan`/`:actuation/contact-guardian`
  proposals NEVER auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/finalize-support-plan | :actuation/contact-guardian | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [learning.facts :as facts]
            [learning.registry :as registry]
            [learning.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the learner, jurisdiction or cohort assignment. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "学習者記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :learner/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-studyplan
  "Per-jurisdiction educational-support/student-data-privacy evidence
  checklist draft. `:no-spec?` injects the failure mode we must defend
  against: proposing a checklist for a jurisdiction with NO official
  spec-basis in `learning.facts` -- the Learner Safety Governor must
  reject this (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [l (store/learner db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction l))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "learning.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :studyplan/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :studyplan/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-dropout-risk
  "Dropout-risk screening draft. `:dropout-risk-unresolved?` on the
  learner record injects the failure mode: the Learner Safety Governor
  must HOLD, un-overridably, on any unresolved risk."
  [db {:keys [subject]}]
  (let [l (store/learner db subject)]
    (cond
      (nil? l)
      {:summary "対象学習者記録が見つかりません" :rationale "no learner record"
       :cites [] :effect :dropout-risk-screen/set :value {:learner-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:dropout-risk-unresolved? l))
      {:summary    (str (:learner-name l) ": 未解決の中退リスクを検出")
       :rationale  "スクリーニングが未解決の中退リスクを検出。人手確認とホールドが必須。"
       :cites      [:dropout-risk-check]
       :effect     :dropout-risk-screen/set
       :value      {:learner-id subject :verdict :unresolved}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:learner-name l) ": 未解決の中退リスクなし")
       :rationale  "中退リスクスクリーニング完了。"
       :cites      [:dropout-risk-check]
       :effect     :dropout-risk-screen/set
       :value      {:learner-id subject :verdict :resolved}
       :stake      nil
       :confidence 0.9})))

(defn- propose-support-plan
  "Draft the actual SUPPORT-PLAN action -- finalizing a real support
  plan for a learner. ALWAYS `:stake :actuation/finalize-support-
  plan` -- this is a REAL-WORLD learning-support act, never a draft
  the actor may auto-run. See README `Actuation`: no phase ever adds
  this op to a phase's `:auto` set (`learning.phase`); the governor
  also always escalates on `:actuation/finalize-support-plan`. Two
  independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [l (store/learner db subject)]
    {:summary    (str subject " 向け支援計画確定提案"
                      (when l (str " (learner=" (:learner-name l) ")")))
     :rationale  (if l
                   (str "cohort-learner-count=" (:cohort-learner-count l)
                        " cohort-tutor-count=" (:cohort-tutor-count l))
                   "学習者記録が見つかりません")
     :cites      (if l [subject] [])
     :effect     :learner/mark-plan-finalized
     :value      {:learner-id subject}
     :stake      :actuation/finalize-support-plan
     :confidence (if (and l (not (registry/learner-to-tutor-ratio-exceeds-maximum? l))) 0.9 0.3)}))

(defn- propose-guardian-contact
  "Draft the actual GUARDIAN-CONTACT action -- contacting a real
  learner's guardian. ALWAYS `:stake :actuation/contact-guardian` --
  this is a REAL-WORLD learning-support act, never a draft the actor
  may auto-run. See README `Actuation`: no phase ever adds this op to
  a phase's `:auto` set (`learning.phase`); the governor also always
  escalates on `:actuation/contact-guardian`. Two independent layers
  agree, deliberately."
  [db {:keys [subject]}]
  (let [l (store/learner db subject)]
    {:summary    (str subject " 向け保護者連絡提案"
                      (when l (str " (learner=" (:learner-name l) ")")))
     :rationale  (if l
                   "consent-record referenced"
                   "学習者記録が見つかりません")
     :cites      (if l [subject] [])
     :effect     :learner/mark-guardian-contacted
     :value      {:learner-id subject}
     :stake      :actuation/contact-guardian
     :confidence (if l 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :learner/intake                       (normalize-intake db request)
    :studyplan/verify                     (verify-studyplan db request)
    :dropout-risk/screen                  (screen-dropout-risk db request)
    :actuation/finalize-support-plan       (propose-support-plan db request)
    :actuation/contact-guardian            (propose-guardian-contact db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは地域学習支援事業の支援計画確定・保護者連絡エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。説明や前置きは"
       "一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:learner/upsert|:studyplan/set|:dropout-risk-screen/set|"
       ":learner/mark-plan-finalized|:learner/mark-guardian-contacted) "
       ":stake(:actuation/finalize-support-plan か :actuation/contact-guardian か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :studyplan/verify                     {:learner (store/learner st subject)}
    :dropout-risk/screen                  {:learner (store/learner st subject)}
    :actuation/finalize-support-plan       {:learner (store/learner st subject)}
    :actuation/contact-guardian            {:learner (store/learner st subject)}
    {:learner (store/learner st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Learner Safety Governor
  escalates/holds -- an LLM hiccup can never auto-finalize a support
  plan or auto-contact a guardian."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :learningadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
