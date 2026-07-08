(ns sports.facts
  "Per-jurisdiction sports-and-recreation-education regulatory catalog
  -- the G2-style spec-basis table the Instruction Safety Governor
  checks every `:program/verify` proposal against ('did the advisor
  cite an OFFICIAL public source for this jurisdiction's sports-
  instruction framework, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official sports-
  governance/coaching-safeguarding authority (see `:provenance`); they
  are a STARTING catalog, not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the
  enrollment-consent/program-curriculum/attendance-hours/background-
  check-clearance evidence set this blueprint's own Offer names;
  `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any `:actuation/finalize-
  certification` proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "スポーツ庁 (Japan Sports Agency, MEXT)"
          :legal-basis "スポーツ基本法 (Sport Basic Act, Act No. 78 of 2011)"
          :national-spec "スポーツ指導者資格制度および安全確保のための指導基準"
          :provenance "https://www.mext.go.jp/sports/"
          :required-evidence ["参加同意記録 (enrollment-consent-record)"
                              "プログラム記録 (program-curriculum-record)"
                              "出席時間記録 (attendance-hours-record)"
                              "身元確認記録 (background-check-clearance-record)"]}
   "USA" {:name "United States"
          :owner-authority "U.S. Center for SafeSport"
          :legal-basis "Protecting Young Victims from Sexual Abuse and Safe Sport Authorization Act of 2017 (SafeSport Act)"
          :national-spec "Athlete-facing coach background-screening and abuse-prevention training requirements"
          :provenance "https://uscenterforsafesport.org/"
          :required-evidence ["Enrollment consent record"
                              "Program-curriculum record"
                              "Attendance-hours record"
                              "Background-check-clearance record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "UK Coaching / Disclosure and Barring Service (DBS)"
          :legal-basis "Safeguarding Vulnerable Groups Act 2006"
          :national-spec "UK Coaching Framework and DBS-checked coach requirements"
          :provenance "https://www.ukcoaching.org/"
          :required-evidence ["Enrollment consent record"
                              "Program-curriculum record"
                              "Attendance-hours record"
                              "Background-check-clearance record"]}
   "DEU" {:name "Germany"
          :owner-authority "Deutscher Olympischer Sportbund (DOSB)"
          :legal-basis "Rahmenrichtlinien für Qualifizierung im Sport (DOSB) / §72a Achtes Buch Sozialgesetzbuch (SGB VIII)"
          :national-spec "Trainerlizenzierung und erweitertes Führungszeugnis für Übungsleiter"
          :provenance "https://www.dosb.de/"
          :required-evidence ["Einwilligungsprotokoll (enrollment-consent-record)"
                              "Programmprotokoll (program-curriculum-record)"
                              "Anwesenheitsstundenprotokoll (attendance-hours-record)"
                              "Führungszeugnisprotokoll (background-check-clearance-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to finalize a
  certification on it."
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
      :note (str "cloud-itonami-isic-8541 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `sports.facts/catalog`, "
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
