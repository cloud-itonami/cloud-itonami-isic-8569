(ns learning.facts
  "Per-jurisdiction educational-support/student-data-privacy regulatory
  catalog -- the G2-style spec-basis table the Learner Safety Governor
  checks every `:studyplan/verify` proposal against ('did the advisor
  cite an OFFICIAL public source for this jurisdiction's educational-
  support and student-data-privacy framework, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official education
  ministry/department and student-data-protection law (see
  `:provenance`); they are a STARTING catalog, not a from-scratch
  survey of all ~194 jurisdictions. Extending coverage is additive:
  add one map to `catalog`, cite a real source, done -- never invent a
  jurisdiction's requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  consent-record/guardian-consent-record/learner-data-purpose-
  limitation-record/support-plan-record evidence set every prior
  sibling's evidence checklist submits in some form; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:actuation/finalize-support-plan`/`:actuation/
  contact-guardian` proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "文部科学省 (Ministry of Education, Culture, Sports, Science and Technology)"
          :legal-basis "個人情報の保護に関する法律 (APPI) / 教育情報セキュリティポリシーに関するガイドライン"
          :national-spec "学習支援事業者における学習者情報の取扱いおよび保護者連絡に関する要件"
          :provenance "https://www.mext.go.jp/a_menu/shotou/zyouhou/index.htm"
          :required-evidence ["同意記録 (consent-record)"
                              "保護者同意記録 (guardian-consent-record)"
                              "学習者情報目的制限記録 (learner-data-purpose-limitation-record)"
                              "支援計画記録 (support-plan-record)"]}
   "USA" {:name "United States"
          :owner-authority "U.S. Department of Education"
          :legal-basis "Family Educational Rights and Privacy Act (FERPA), 20 U.S.C. § 1232g"
          :national-spec "Community learning-support provider student-record privacy and guardian-notification requirements"
          :provenance "https://studentprivacy.ed.gov/ferpa"
          :required-evidence ["Consent record"
                              "Guardian-consent record"
                              "Learner-data purpose-limitation record"
                              "Support-plan record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Department for Education (DfE) / Information Commissioner's Office (ICO)"
          :legal-basis "UK GDPR + Data Protection Act 2018 (pupil data, special category where applicable)"
          :national-spec "Educational-support provider pupil-data handling and parental-consent requirements"
          :provenance "https://www.gov.uk/government/organisations/department-for-education"
          :required-evidence ["Consent record"
                              "Guardian-consent record"
                              "Learner-data purpose-limitation record"
                              "Support-plan record"]}
   "DEU" {:name "Germany"
          :owner-authority "Kultusministerkonferenz (KMK) / Landesdatenschutzbehörden"
          :legal-basis "Landesschulgesetze (state school acts) / Datenschutz-Grundverordnung (DSGVO, Schülerdaten)"
          :national-spec "Registrierung von Lernförderungsanbietern und Anforderungen an Schülerdaten- und Erziehungsberechtigtenkontakt"
          :provenance "https://www.kmk.org/themen/allgemeinbildende-schulen.html"
          :required-evidence ["Einwilligungsprotokoll (consent-record)"
                              "Erziehungsberechtigteneinwilligung (guardian-consent-record)"
                              "Zweckbindungsprotokoll Schülerdaten (learner-data-purpose-limitation-record)"
                              "Förderplanprotokoll (support-plan-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to finalize a
  support plan or contact a guardian on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-8569 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `learning.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
