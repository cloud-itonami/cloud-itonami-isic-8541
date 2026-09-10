(ns sports.governor
  "Instruction Safety Governor -- the independent compliance layer that
  earns the CoachOps-LLM the right to commit. The LLM has no notion of
  jurisdictional sports-instruction law, whether a participant's own
  recorded attendance hours actually stay above their own recorded
  minimum requirement, whether a coaching staff member's own
  background check actually stayed cleared, or when an act stops
  being a draft and becomes a real-world certification finalization,
  so this MUST be a separate system able to *reject* a proposal and
  fall back to HOLD -- the sports-and-recreation-education analog of
  `cloud-itonami-isic-8620`'s ClinicGovernor.

  Five checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence,
  attendance hours below a participant's own recorded minimum, an
  uncleared background check, or a double certification). The
  confidence/actuation gate is SOFT: it asks a human to look (low
  confidence / actuation), and the human may approve -- but see
  `sports.phase`: for `:stake :actuation/finalize-certification` (a
  real certification finalization) NO phase ever allows auto-commit
  either. Two independent layers agree that actuation is always a
  human call.

    1. Spec-basis                  -- did the program proposal cite an
                                       OFFICIAL source (`sports.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/finalize-
                                       certification`, has the
                                       participant actually been
                                       assessed with a full enrollment-
                                       consent-record/program-
                                       curriculum-record/attendance-
                                       hours-record/background-check-
                                       clearance-record evidence
                                       checklist on file?
    3. Attendance hours
       insufficient                   -- for `:actuation/finalize-
                                       certification`, INDEPENDENTLY
                                       recompute whether the
                                       participant's own accumulated
                                       attendance hours fall short of
                                       their own recorded minimum
                                       (`sports.registry/attendance-
                                       hours-insufficient?`) -- needs
                                       no proposal inspection at all.
                                       Literally reuses `secondary.
                                       registry/attendance-hours-
                                       insufficient?`'s exact concept
                                       and field names -- the SAME
                                       real-world requirement (minimum
                                       instructional hours before a
                                       final credential) genuinely
                                       recurs in this education
                                       vertical. The EIGHTH instance of
                                       this fleet's MINIMUM-threshold
                                       sufficiency check family.
    4. Background check not
       cleared                        -- reported by THIS proposal
                                       itself (a `:background-check/
                                       screen` that just found an
                                       uncleared check), or already on
                                       file for the participant
                                       (`:background-check/screen`/
                                       `:actuation/finalize-
                                       certification`). Evaluated
                                       UNCONDITIONALLY (not scoped to a
                                       specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/...(forty-
                                       two prior siblings, most
                                       recently `nursing.governor/
                                       credential-not-current-
                                       violations`)...established -- a
                                       LITERAL reuse of `school.
                                       governor/background-check-not-
                                       cleared-violations`'s own
                                       concept (the SECOND instance),
                                       not claimed as new, directly
                                       grounded in this blueprint's own
                                       operator-guide text.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       finalize-certification` (a REAL
                                       certification act) -> escalate.

  One more guard, double-certification prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-certified-violations` refuses to
  finalize a certification for the SAME participant twice, off a
  dedicated `:certified?` fact (never a `:status` value) -- the SAME
  'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [sports.facts :as facts]
            [sports.registry :as registry]
            [sports.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Finalizing a real certification or safety-relevant progress record
  is the ONE real-world actuation event this actor performs -- a
  single-member set, matching `cloud-itonami-isic-6511`'s/`6621`'s/
  `6629`'s/`6612`'s/`6492`'s/`7120`'s/`8620`'s single-actuation shape."
  #{:actuation/finalize-certification})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:program/verify` (or `:actuation/finalize-certification`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's sports-instruction requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:program/verify :actuation/finalize-certification} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は指導基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/finalize-certification`, the jurisdiction's required
  enrollment-consent-record/program-curriculum-record/attendance-
  hours-record/background-check-clearance-record evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-certification)
    (let [p (store/participant st subject)
          program (store/program-of st subject)]
      (when-not (and program
                     (facts/required-evidence-satisfied?
                      (:jurisdiction p) (:checklist program)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(参加同意記録/プログラム記録/出席時間記録/身元確認記録等)が充足していない状態での提案"}]))))

(defn- attendance-hours-insufficient-violations
  "For `:actuation/finalize-certification`, INDEPENDENTLY recompute
  whether the participant's own accumulated attendance hours fall
  short of their own recorded minimum via `sports.registry/
  attendance-hours-insufficient?` -- needs no proposal inspection at
  all, since its inputs are permanent ground-truth fields already on
  the participant."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-certification)
    (let [p (store/participant st subject)]
      (when (registry/attendance-hours-insufficient? p)
        [{:rule :attendance-hours-insufficient
          :detail (str subject " の出席時間(" (:attendance-hours-completed p)
                      ")が必要時間(" (:attendance-hours-required p) ")に不足")}]))))

(defn- background-check-not-cleared-violations
  "An uncleared background check -- reported by THIS proposal (e.g. a
  `:background-check/screen` that itself just found an uncleared
  check), or already on file in the store for the participant
  (`:background-check/screen`/`:actuation/finalize-certification`) --
  is a HARD, un-overridable hold. Evaluated UNCONDITIONALLY (not
  scoped to a specific op) so the screening op itself can HARD-hold on
  its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :not-cleared (get-in proposal [:value :verdict]))
        participant-id (when (contains? #{:background-check/screen :actuation/finalize-certification} op) subject)
        hit-on-file? (and participant-id (= :not-cleared (:verdict (store/background-check-of st participant-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :background-check-not-cleared
        :detail "指導者の身元確認が未完了の状態での資格確定提案は進められない"}])))

(defn- already-certified-violations
  "For `:actuation/finalize-certification`, refuses to finalize a
  certification for the SAME participant twice, off a dedicated
  `:certified?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-certification)
    (when (store/participant-already-certified? st subject)
      [{:rule :already-certified
        :detail (str subject " は既に資格確定済み")}])))

(defn check
  "Censors a CoachOps-LLM proposal against the governor rules. Returns
  {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (attendance-hours-insufficient-violations request st)
                           (background-check-not-cleared-violations request proposal st)
                           (already-certified-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
