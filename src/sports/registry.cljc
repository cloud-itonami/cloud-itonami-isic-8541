(ns sports.registry
  "Pure-function certification-finalization record construction -- an
  append-only sports-and-recreation-education book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a certification-finalization
  reference number -- every academy/jurisdiction assigns its own
  reference format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `sports.facts` uses.

  `attendance-hours-insufficient?` reuses `secondary.registry/
  attendance-hours-insufficient?`'s exact concept and field names
  literally -- the SAME real-world requirement (a minimum number of
  instructional/attendance hours before a final credential can be
  issued) genuinely recurs in this sports-and-recreation-EDUCATION
  vertical, since `8541` is itself an education ISIC class. The EIGHTH
  instance of this fleet's MINIMUM-threshold sufficiency check family
  (`veterinary`/`funeral`/`hospital` established the first three,
  temporal; `association`/`secondary`/`polling`/`research` generalized
  it to non-temporal ground truths as the fourth through seventh).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real academy-management system. It builds the RECORD an
  academy would keep, not the act of finalizing the certification
  itself (that is `sports.operation`'s `:actuation/finalize-
  certification`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  academy's own act, not this actor's. See README `Actuation`."
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

(defn attendance-hours-insufficient?
  "Does `participant`'s own `:attendance-hours-completed` fall short of
  the jurisdiction's own recorded `:attendance-hours-required` minimum?
  A pure ground-truth check against the participant's own permanent
  fields -- no upstream comparison needed. The EIGHTH instance of this
  fleet's MINIMUM-threshold sufficiency check family (see ns
  docstring)."
  [{:keys [attendance-hours-completed attendance-hours-required]}]
  (and (number? attendance-hours-completed) (number? attendance-hours-required)
       (< attendance-hours-completed attendance-hours-required)))

(defn register-certification-finalization
  "Validate + construct the CERTIFICATION-FINALIZATION registration
  DRAFT -- the academy's own act of finalizing a real certification or
  safety-relevant progress record. Pure function -- does not touch any
  real academy-management system; it builds the RECORD an academy
  would keep. `sports.governor` independently re-verifies the
  participant's own attendance-hours ground truth and background-check
  clearance, and blocks a double-finalization for the same participant,
  before this is ever allowed to commit."
  [participant-id jurisdiction sequence]
  (when-not (and participant-id (not= participant-id ""))
    (throw (ex-info "certification-finalization: participant_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "certification-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "certification-finalization: sequence must be >= 0" {})))
  (let [certification-number (str (str/upper-case jurisdiction) "-CRT-" (zero-pad sequence 6))
        record {"record_id" certification-number
                "kind" "certification-finalization-draft"
                "participant_id" participant-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "certification_number" certification-number
     "certificate" (unsigned-certificate "CertificationFinalization" certification-number certification-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
