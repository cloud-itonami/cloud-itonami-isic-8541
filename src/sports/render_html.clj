(ns sports.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 for `cloud-itonami-isic-8541`: this
  repo previously had NO demo page and no generator at all (there was
  no `docs/samples/` directory). This namespace drives the REAL actor
  stack -- `sports.operation` (a langgraph-clj StateGraph) ->
  `sports.governor` (Instruction Safety Governor) -> `sports.store`
  (MemStore SSoT) -- through a scenario extended from this repo's own
  `sports.sim` demo driver (`clojure -M:dev:run`, run BEFORE this file
  was written to confirm the real seeded participant ids
  `participant-1`..`participant-4` and the real ledger shape).

  Every value on the page is read back out of that live run: the
  participant directory, the audit ledger, the governor's own
  `:rule`/`:detail` strings, the drafted certification records, the
  jurisdiction catalog in `sports.facts`, and the phase gate table in
  `sports.phase`. Nothing on the page is hand-typed domain data.

  The scenario exercises ALL FIVE of `sports.governor`'s HARD rules
  (`:no-spec-basis`, `:evidence-incomplete`,
  `:attendance-hours-insufficient`, `:background-check-not-cleared`,
  `:already-certified`), one hold in which TWO of them fire at once,
  plus the two adjacent refusal layers that are NOT governor rules --
  the phase gate (`:phase-disabled`) and a human approver's rejection
  (`:approver-rejected`).

  Deterministic: no timestamps, no random, no wall-clock in the page
  content; two consecutive runs are byte-identical. `-main` REFUSES to
  write a page produced by a run with zero `:governor-hold` records --
  the HARD-hold requirement is a build-time invariant, not a
  convention, so a future regression that silently stops holding fails
  the build instead of publishing a page that claims safety it no
  longer has.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [langgraph.graph :as g]
            [sports.facts :as facts]
            [sports.governor :as governor]
            [sports.operation :as op]
            [sports.phase :as phase]
            [sports.store :as store]))

(def ^:private operator
  "The human operator identity injected as the graph's `:context`.
  `:phase 3` is `sports.phase/default-phase` -- the most permissive
  rollout phase this actor has, which is what makes the holds below
  meaningful: they are not an artifact of a restrictive phase."
  {:actor-id "op-1" :actor-role :licensed-educator :phase 3})

;; ----------------------------- the real run -----------------------------
;;
;; `:approval-granted` audit facts are emitted by `sports.operation`'s
;; `:request-approval` node into the graph's `:audit` channel, but the
;; `:commit` node only appends its own `:committed` fact to the store
;; ledger -- so approver identity never reaches `store/ledger`
;; (measured, see `approver-rows` below). We therefore keep each
;; thread's FINAL audit channel, which is the only place that evidence
;; exists. Keyed by thread-id, insertion-ordered, last write wins, so a
;; resumed (approved/rejected) run replaces its own pre-interrupt
;; snapshot instead of being counted twice.

(defn- record-audit! [runs tid state]
  (swap! runs (fn [{:keys [order by-tid]}]
                {:order  (if (contains? by-tid tid) order (conj order tid))
                 :by-tid (assoc by-tid tid (vec (:audit state)))})))

(defn- exec! [actor runs tid request]
  (let [r (g/run* actor {:request request :context operator} {:thread-id tid})]
    (record-audit! runs tid (:state r))
    r))

(defn- exec-ctx! [actor runs tid request ctx]
  (let [r (g/run* actor {:request request :context ctx} {:thread-id tid})]
    (record-audit! runs tid (:state r))
    r))

(defn- approve! [actor runs tid]
  (let [r (g/run* actor {:approval {:status :approved :by "op-1"}}
                  {:thread-id tid :resume? true})]
    (record-audit! runs tid (:state r))
    r))

(defn- reject! [actor runs tid]
  (let [r (g/run* actor {:approval {:status :rejected :by "op-1"}}
                  {:thread-id tid :resume? true})]
    (record-audit! runs tid (:state r))
    r))

(defn run-demo!
  "Runs a freshly seeded MemStore through a scenario that reaches every
  disposition this actor can produce. Returns `{:db .. :audit [..]}`
  where `:audit` is every graph audit fact, in thread order.

  participant-1 (JPN, 40h/30h required, background cleared) walks the
  full clean lifecycle: intake auto-commits (the ONLY op in phase 3's
  `:auto` set), then program verification, background-check screening
  and finally the certification finalization -- each escalating to a
  human, each approved, the last producing certification JPN-CRT-000000.

  Then the refusals, none of which are hand-authored -- each is
  whatever the governor actually returned:

    :no-spec-basis                 participant-2's program verification
                                   is asked for jurisdiction ATL, which
                                   `sports.facts` deliberately does not
                                   cover. The advisor declines to invent
                                   requirements and the governor HARD-
                                   holds the empty citation.
    :evidence-incomplete           participant-2's certification is then
                                   attempted with no program record on
                                   file at all.
    :evidence-incomplete +         participant-3's certification is
    :attendance-hours-insufficient attempted BEFORE its program is
                                   verified -- two independent HARD
                                   rules fire in one hold.
    :attendance-hours-insufficient the same participant AFTER its
                                   program is on file: the evidence rule
                                   clears, and the independently
                                   recomputed 15h < 30h shortfall still
                                   holds it.
    :background-check-not-cleared  participant-4's screening detects its
                                   own uncleared check and HARD-holds on
                                   its own finding.
    :already-certified             participant-1's certification is
                                   attempted a second time.

  Two adjacent layers that are NOT governor rules are also exercised:
  the phase gate refuses a participant intake replayed at phase 0
  (`:phase-disabled`), and a human approver REJECTS participant-4's
  certification (`:approver-rejected`). That last one is worth reading
  closely: participant-4's screening HARD-held, so no background-check
  record was ever committed, so the governor's on-file check has
  nothing to see and the proposal reaches a human -- the human is the
  backstop, exactly as the two-layer design intends."
  []
  (let [db    (store/seed-db)
        actor (op/build db)
        runs  (atom {:order [] :by-tid {}})]

    ;; participant-1 -- full clean lifecycle through to a real certification
    (exec! actor runs "t1-intake" {:op :participant/intake :subject "participant-1"
                                   :patch {:id "participant-1" :participant-name "Sato Kenji"}})
    (exec! actor runs "t1-program" {:op :program/verify :subject "participant-1"})
    (approve! actor runs "t1-program")
    (exec! actor runs "t1-bg" {:op :background-check/screen :subject "participant-1"})
    (approve! actor runs "t1-bg")
    (exec! actor runs "t1-cert" {:op :actuation/finalize-certification :subject "participant-1"})
    (approve! actor runs "t1-cert")

    ;; participant-2 -- unregistered jurisdiction, then no evidence on file
    (exec! actor runs "t2-program" {:op :program/verify :subject "participant-2" :no-spec? true})
    (exec! actor runs "t2-cert" {:op :actuation/finalize-certification :subject "participant-2"})

    ;; participant-3 -- two rules at once, then the attendance shortfall alone
    (exec! actor runs "t3-cert-early" {:op :actuation/finalize-certification :subject "participant-3"})
    (exec! actor runs "t3-program" {:op :program/verify :subject "participant-3"})
    (approve! actor runs "t3-program")
    (exec! actor runs "t3-cert" {:op :actuation/finalize-certification :subject "participant-3"})

    ;; participant-4 -- screening HARD-holds on its own finding; the later
    ;; certification therefore reaches a human, who rejects it
    (exec! actor runs "t4-bg" {:op :background-check/screen :subject "participant-4"})
    (exec! actor runs "t4-program" {:op :program/verify :subject "participant-4"})
    (approve! actor runs "t4-program")
    (exec! actor runs "t4-cert" {:op :actuation/finalize-certification :subject "participant-4"})
    (reject! actor runs "t4-cert")

    ;; double certification
    (exec! actor runs "t1-cert-again" {:op :actuation/finalize-certification :subject "participant-1"})

    ;; the phase gate, an independent layer from the governor
    (exec-ctx! actor runs "t0-phase0"
               {:op :participant/intake :subject "participant-2" :patch {:id "participant-2"}}
               (assoc operator :phase 0))

    (let [{:keys [order by-tid]} @runs]
      {:db db :audit (vec (mapcat by-tid order))})))

;; ----------------------------- html helpers -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- nm [v] (if (keyword? v) (name v) (str v)))

(defn- code [v] (str "<code>" (esc v) "</code>"))

(defn- pill [class label] (str "<span class=\"pill " class "\">" (esc label) "</span>"))

(defn- row [& cells]
  (str "        <tr>" (str/join (map #(str "<td>" % "</td>") cells)) "</tr>"))

(defn- table [headers rows]
  (str "    <table>\n"
       "      <thead><tr>" (str/join (map #(str "<th>" (esc %) "</th>") headers)) "</tr></thead>\n"
       "      <tbody>\n" (str/join "\n" rows) "\n      </tbody>\n"
       "    </table>\n"))

(defn- section [title lede body]
  (str "  <section class=\"card\">\n"
       "    <h2>" (esc title) "</h2>\n"
       "    <p class=\"muted\">" lede "</p>\n"
       body
       "  </section>\n"))

;; ----------------------------- derived rows -----------------------------

(defn- hold-facts [ledger]
  (filterv #(#{:governor-hold :approval-rejected} (:t %)) ledger))

(defn- governor-holds [ledger]
  (filterv #(= :governor-hold (:t %)) ledger))

(defn- last-fact-for [ledger id]
  (last (filter #(= (:subject %) id) ledger)))

(defn- status-cell [ledger id]
  (let [f (last-fact-for ledger id)]
    (cond
      (nil? f) (pill "muted" "no activity")
      (= :committed (:t f)) (pill "ok" "committed")
      (= :approval-rejected (:t f)) (pill "warn" "rejected by approver")
      (= :governor-hold (:t f))
      (if-let [rules (seq (map :rule (:violations f)))]
        (pill "bad" (str "HARD hold · " (str/join " + " (map nm rules))))
        (pill "bad" (str "held · " (nm (:phase-reason f "phase gate")))))
      :else (pill "muted" (nm (:t f))))))

(defn- participant-row [ledger {:keys [id participant-name jurisdiction
                                       attendance-hours-completed attendance-hours-required
                                       background-check-cleared? certified? certification-number]}]
  (row (code id)
       (esc participant-name)
       (esc jurisdiction)
       (str (esc attendance-hours-completed) " / " (esc attendance-hours-required)
            (if (< attendance-hours-completed attendance-hours-required)
              (str " " (pill "bad" "short"))
              (str " " (pill "ok" "met"))))
       (if background-check-cleared? (pill "ok" "cleared") (pill "bad" "not cleared"))
       (if certified? (pill "ok" (str "certified · " certification-number)) (pill "muted" "not certified"))
       (status-cell ledger id)))

(defn- jurisdiction-row [[iso3 {:keys [name owner-authority legal-basis provenance required-evidence]}]]
  (row (code iso3)
       (esc name)
       (esc owner-authority)
       (esc legal-basis)
       (str (count required-evidence) " items")
       (str "<a href=\"" (esc provenance) "\">" (esc provenance) "</a>")))

(defn- gate-row [op]
  (let [ordered   (sort (keys phase/phases))
        writes-at (first (filter #(contains? (:writes (phase/phases %)) op) ordered))
        auto-at   (filterv #(contains? (:auto (phase/phases %)) op) ordered)
        high?     (contains? governor/high-stakes op)]
    (row (code op)
         (if writes-at (str "phase " writes-at "+") (pill "bad" "never"))
         (if (seq auto-at)
           (pill "ok" (str "phase " (str/join ", " auto-at)))
           (pill "warn" "never — always human"))
         (if high?
           (pill "bad" "high-stakes · escalates even when governor-clean")
           (pill "muted" "—")))))

(defn- rule-row [ledger rule]
  (let [hits (filterv (fn [f] (some #(= rule (:rule %)) (:violations f))) (hold-facts ledger))
        detail (->> hits first :violations (filter #(= rule (:rule %))) first :detail)]
    (row (code rule)
         (str (count hits))
         (str/join ", " (map #(code (:subject %)) hits))
         (esc detail))))

(defn- hold-row [{:keys [t op subject violations phase-reason phase confidence]}]
  (row (if (= :approval-rejected t) (pill "warn" (nm t)) (pill "bad" (nm t)))
       (code op)
       (code subject)
       (if (seq violations)
         (str/join " " (map #(code (:rule %)) violations))
         (pill "warn" (str (nm phase-reason) " (phase " phase ")")))
       (esc confidence)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (row (case t
         :committed (pill "ok" "committed")
         :governor-hold (pill "bad" "governor-hold")
         :approval-rejected (pill "warn" "approval-rejected")
         (pill "muted" (nm t)))
       (code op)
       (code subject)
       (esc (nm (or disposition "")))
       (if (seq basis)
         (esc (str/join ", " (map nm basis)))
         "<span class=\"muted\">—</span>")))

(defn- certification-row [rec]
  (row (code (get rec "record_id"))
       (esc (get rec "kind"))
       (code (get rec "participant_id"))
       (esc (get rec "jurisdiction"))
       (if (get rec "immutable") (pill "ok" "immutable") (pill "warn" "mutable"))))

;; ----------------------------- approver attribution (derived) -----------------------------
;;
;; MEASURED on this repo, not assumed: `sports.operation`'s
;; `:request-approval` node attaches `:approved-by` to the record's
;; `:payload` only. `sports.store/commit-record!` reads `:payload` for
;; `:program/set` and `:background-check/set` (so the approver DOES
;; survive there) but `:participant/mark-certified` reads NEITHER
;; `:value` NOR `:payload` -- it re-drafts the record through
;; `sports.registry/register-certification-finalization`, whose only
;; inputs are participant-id, jurisdiction and sequence. So the approver
;; of the one real-world actuation is dropped. It is also absent from
;; `store/ledger`, because the `:approval-granted` fact is only ever
;; written to the graph's `:audit` channel.
;;
;; The disclosure below is DERIVED at render time -- it asks each
;; committed record whether the approver key is actually present, and
;; falls back to the audit join only when it is not. If the store is
;; later fixed to persist the approver, this table reports the fix
;; instead of continuing to claim a defect.

(defn- approval-granted-by
  "The approver recorded in the graph audit channel for `op` on
  `subject`, or nil."
  [audit op subject]
  (some (fn [f] (when (and (= :approval-granted (:t f))
                           (= op (:op f))
                           (= subject (:subject f)))
                  (:by f)))
        audit))

(defn- attribution-row [label committed-record present? audit-approver]
  (let [in-record (when present? (present? committed-record))]
    (row (esc label)
         (cond (nil? committed-record) (pill "muted" "no committed record")
               in-record               (pill "ok" (str "in commit record · " in-record))
               :else                   (pill "bad" "absent from commit record"))
         (if audit-approver
           (code audit-approver)
           "<span class=\"muted\">—</span>")
         (cond
           (nil? committed-record) (esc "nothing committed for this record on this run")
           in-record               (esc "approver survives into the SSoT")
           audit-approver          (pill "warn" "audit only — not in commit record")
           :else                   (esc "no approval on this path (auto-commit or never approved)")))))

(defn- approver-rows [db audit]
  (let [program (store/program-of db "participant-1")
        bg      (store/background-check-of db "participant-1")
        cert    (first (store/certification-history db))
        part    (store/participant db "participant-1")]
    [(attribution-row ":program/set → programs register (participant-1)"
                      program #(:approved-by %)
                      (approval-granted-by audit :program/verify "participant-1"))
     (attribution-row ":background-check/set → background-checks register (participant-1)"
                      bg #(:approved-by %)
                      (approval-granted-by audit :background-check/screen "participant-1"))
     (attribution-row ":participant/mark-certified → certification record (participant-1)"
                      cert #(get % "approved_by")
                      (approval-granted-by audit :actuation/finalize-certification "participant-1"))
     (attribution-row ":participant/mark-certified → participant record (participant-1)"
                      part #(:approved-by %)
                      (approval-granted-by audit :actuation/finalize-certification "participant-1"))]))

;; ----------------------------- style -----------------------------
;;
;; DADS primitives, extracted verbatim from this repo's own vendored
;; copy in `docs/index.html` (jp-go-digital-design-system, upstream
;; 3b34f4c3553fa3bee90bfd8b6fe962ac3055107d, MIT (c) 2025 デジタル庁) so
;; the console matches the product face and the build stays offline --
;; no git dep, no network. Only the primitives actually referenced
;; below are inlined. Tint backgrounds use the `-50` primitive steps:
;; the semantic `-1`/`-2` error tokens are BOTH dark and are not a
;; strong/weak pair.

(def ^:private console-css "
:root {
  --color-neutral-white: #ffffff;
  --color-neutral-solid-gray-50: #f2f2f2;
  --color-neutral-solid-gray-100: #e6e6e6;
  --color-neutral-solid-gray-200: #cccccc;
  --color-neutral-solid-gray-600: #666666;
  --color-neutral-solid-gray-700: #4d4d4d;
  --color-neutral-solid-gray-900: #1a1a1a;
  --color-primitive-blue-50: #e8f1fe;
  --color-primitive-blue-200: #c5d7fb;
  --color-primitive-blue-900: #0017c1;
  --color-primitive-blue-1000: #00118f;
  --color-primitive-blue-1200: #000060;
  --color-primitive-red-50: #fdeeee;
  --color-primitive-red-200: #ffbbbb;
  --color-primitive-red-1000: #a90000;
  --color-primitive-green-50: #e6f5ec;
  --color-primitive-green-100: #c2e5d1;
  --color-primitive-green-900: #115a36;
  --color-primitive-orange-50: #ffeee2;
  --color-primitive-orange-100: #ffdfca;
  --color-primitive-orange-900: #ac3e00;
  --font-family-sans: \"Noto Sans JP\", -apple-system, BlinkMacSystemFont, sans-serif;
  --font-family-mono: \"Noto Sans Mono\", monospace;
}
* { box-sizing: border-box; }
body {
  margin: 0;
  font-family: var(--font-family-sans);
  color: var(--color-neutral-solid-gray-900);
  background: var(--color-neutral-solid-gray-50);
  line-height: 1.7;
}
header.bar {
  background: var(--color-primitive-blue-1200);
  color: var(--color-neutral-white);
  padding: 24px 32px;
}
header.bar h1 { margin: 0 0 8px; font-size: 20px; line-height: 1.5; }
header.bar .badge {
  display: inline-block;
  font-size: 13px;
  background: var(--color-primitive-blue-1000);
  border: 1px solid var(--color-primitive-blue-900);
  border-radius: 4px;
  padding: 2px 10px;
}
main { padding: 24px 32px 64px; max-width: 1200px; }
section.card {
  background: var(--color-neutral-white);
  border: 1px solid var(--color-neutral-solid-gray-200);
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 24px;
}
section.card h2 {
  margin: 0 0 4px;
  font-size: 17px;
  border-bottom: 2px solid var(--color-primitive-blue-1200);
  padding-bottom: 8px;
}
p.muted, span.muted { color: var(--color-neutral-solid-gray-600); }
p.muted { font-size: 13px; margin: 8px 0 16px; }
table { border-collapse: collapse; width: 100%; font-size: 13px; }
th, td {
  text-align: left;
  padding: 7px 10px;
  border-bottom: 1px solid var(--color-neutral-solid-gray-100);
  vertical-align: top;
}
th {
  background: var(--color-neutral-solid-gray-50);
  border-bottom: 1px solid var(--color-neutral-solid-gray-200);
  font-weight: 700;
  white-space: nowrap;
}
tr:last-child td { border-bottom: none; }
code {
  font-family: var(--font-family-mono);
  font-size: 12px;
  background: var(--color-neutral-solid-gray-50);
  border: 1px solid var(--color-neutral-solid-gray-100);
  border-radius: 3px;
  padding: 1px 5px;
}
a { color: var(--color-primitive-blue-1000); }
.pill {
  display: inline-block;
  font-size: 12px;
  border-radius: 10px;
  padding: 1px 9px;
  white-space: nowrap;
  border: 1px solid transparent;
}
.pill.ok {
  background: var(--color-primitive-green-50);
  color: var(--color-primitive-green-900);
  border-color: var(--color-primitive-green-100);
}
.pill.warn {
  background: var(--color-primitive-orange-50);
  color: var(--color-primitive-orange-900);
  border-color: var(--color-primitive-orange-100);
}
.pill.bad {
  background: var(--color-primitive-red-50);
  color: var(--color-primitive-red-1000);
  border-color: var(--color-primitive-red-200);
}
.pill.muted {
  background: var(--color-neutral-solid-gray-50);
  color: var(--color-neutral-solid-gray-700);
  border-color: var(--color-neutral-solid-gray-200);
}
.note {
  background: var(--color-primitive-blue-50);
  border: 1px solid var(--color-primitive-blue-200);
  border-radius: 6px;
  padding: 12px 16px;
  font-size: 13px;
  margin: 0 0 16px;
}
footer {
  padding: 0 32px 48px;
  max-width: 1200px;
  font-size: 12px;
  color: var(--color-neutral-solid-gray-600);
}
")

;; ----------------------------- render -----------------------------

(defn render
  "Renders the operator console from a completed `run-demo!` result."
  [{:keys [db audit]}]
  (let [ledger        (vec (store/ledger db))
        participants  (store/all-participants db)
        certs         (vec (store/certification-history db))
        holds         (hold-facts ledger)
        hard          (governor-holds ledger)
        fired-rules   (->> holds (mapcat :violations) (map :rule) distinct
                           (remove #{:approver-rejected}) sort vec)
        cov           (facts/coverage)]
    (str
     "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n"
     "<meta charset=\"utf-8\">\n"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
     "<meta name=\"color-scheme\" content=\"light\">\n"
     "<title>Operator console · cloud-itonami-isic-8541 · sports and recreation education</title>\n"
     "<style>" console-css "</style>\n"
     "</head>\n<body>\n"

     "<header class=\"bar\">\n"
     "  <h1>Sports &amp; recreation education (ISIC 8541) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · certification finalization is always a human call</span>\n"
     "</header>\n"
     "<main>\n"

     "  <div class=\"note\">Build-time generated by <code>sports.render-html</code> "
     "(<code>clojure -M:dev:render-html</code>) by running the real actor stack — "
     "<code>sports.operation</code> (langgraph StateGraph) → <code>sports.governor</code> → "
     "<code>sports.store</code> — against the seeded participant directory in "
     "<code>sports.store/demo-data</code>. Every row below is read back out of that run. "
     "The build refuses to emit this page if the run produces zero <code>:governor-hold</code> records.</div>\n"

     (section "Participant directory (post-run SSoT state)"
              (str "Final state of the <code>MemStore</code> after the scenario. Attendance is the "
                   "participant's own recorded hours against their own recorded minimum — the figure "
                   "<code>sports.governor</code> independently recomputes rather than trusting the advisor.")
              (table ["Participant" "Name" "Jurisdiction" "Attendance (done / required)"
                      "Background check" "Certification" "Last decision"]
                     (mapv (partial participant-row ledger) participants)))

     (section "Governor HARD rules exercised by this run"
              (str "All five HARD rules in <code>sports.governor</code> fired. A HARD violation cannot be "
                   "overridden by a human approver — the run never reaches the approval node at all. "
                   "Rule names and detail text are the governor's own output, not restated here.")
              (table ["Rule" "Times fired" "Subjects" "Governor detail (verbatim)"]
                     (mapv (partial rule-row ledger) fired-rules)))

     (section "Every refusal this run produced"
              (str "Seven <code>:governor-hold</code> records plus one approver rejection. Note "
                   "<code>participant-3</code>'s first attempt, where two independent HARD rules fire in a "
                   "single hold, and the phase-gate row, which is a different layer entirely: "
                   "<code>sports.phase</code> refuses the write before any rule is even in question.")
              (table ["Fact" "Op" "Subject" "Basis" "Advisor confidence"]
                     (mapv hold-row holds)))

     (section "Action gate (phase rollout × governor stakes)"
              (str "Derived from <code>sports.phase/phases</code> and "
                   "<code>sports.governor/high-stakes</code> at build time. "
                   "<code>:actuation/finalize-certification</code> is absent from every phase's "
                   "<code>:auto</code> set including phase 3, and is independently flagged high-stakes by the "
                   "governor — two layers agree that finalizing a certification is always a human call. "
                   "Confidence floor: <code>" (esc governor/confidence-floor) "</code>.")
              (table ["Op" "Writes enabled" "Auto-commit eligible" "Governor stakes"]
                     (mapv gate-row (sort phase/write-ops))))

     (section "Approver attribution (derived at render time)"
              (str "<code>sports.operation</code> attaches <code>:approved-by</code> to the commit record's "
                   "<code>:payload</code> only. <code>sports.store/commit-record!</code> reads "
                   "<code>:payload</code> for <code>:program/set</code> and "
                   "<code>:background-check/set</code>, but <code>:participant/mark-certified</code> reads "
                   "neither — it re-drafts the record through "
                   "<code>sports.registry/register-certification-finalization</code>, whose inputs are only "
                   "participant-id, jurisdiction and sequence. The approver of the one real-world actuation "
                   "is therefore dropped from the SSoT, and <code>:approval-granted</code> never reaches "
                   "<code>store/ledger</code> either. This table interrogates each committed record for the "
                   "key rather than asserting the defect, so it will report the fix if the store is repaired.")
              (table ["Commit path" "Approver in committed record" "Approver in audit channel" "Disclosure"]
                     (approver-rows db audit)))

     (section "Jurisdiction spec-basis catalog"
              (str "<code>sports.facts/catalog</code> — the citations the governor requires before any "
                   "program verification or certification finalization may commit. Coverage is reported "
                   "honestly: <strong>" (esc (:covered cov)) " of " (esc (:requested cov))
                   "</strong> requested jurisdictions have an official spec-basis. A jurisdiction absent "
                   "from this table has NO spec-basis, and the advisor must not invent one — that is "
                   "exactly what <code>participant-2</code>'s <code>ATL</code> hold above demonstrates.")
              (table ["ISO3" "Jurisdiction" "Owner authority" "Legal basis" "Required evidence" "Provenance"]
                     (mapv jurisdiction-row (sort-by key facts/catalog))))

     (section "Audit ledger (append-only, this run)"
              (str "The complete immutable decision log from <code>store/ledger</code> — every commit and "
                   "every hold, in order. No SSoT mutation happens outside the <code>:commit</code> node.")
              (table ["Fact" "Op" "Subject" "Disposition" "Basis"]
                     (mapv ledger-row ledger)))

     (section "Certification finalization records (drafts)"
              (str "Produced by <code>sports.registry</code>. Every certificate this actor emits is an "
                   "UNSIGNED draft — signature is the academy's own act, not this actor's. The reference "
                   "number is a jurisdiction-scoped sequence; no international check-digit standard is "
                   "invented.")
              (table ["Record id" "Kind" "Participant" "Jurisdiction" "Immutability"]
                     (mapv certification-row certs)))

     "</main>\n"
     "<footer>\n"
     "Generated deterministically — no timestamps or random values in this page, so consecutive builds are "
     "byte-identical. Styling uses jp-go-digital-design-system primitives vendored in this repo's "
     "<code>docs/index.html</code> (MIT, © 2025 デジタル庁).<br>\n"
     "This run: " (esc (count ledger)) " ledger facts · " (esc (count hard))
     " governor holds · " (esc (count fired-rules)) " distinct HARD rules · "
     (esc (count certs)) " certification draft(s).\n"
     "</footer>\n"
     "</body>\n</html>\n")))

(defn -main [& args]
  (let [out    (or (first args) "docs/samples/operator-console.html")
        {:keys [db audit] :as result} (run-demo!)
        ledger (vec (store/ledger db))
        hard   (governor-holds ledger)
        rules  (->> (hold-facts ledger) (mapcat :violations) (map :rule) distinct
                    (remove #{:approver-rejected}) sort vec)]

    ;; Build-time invariant, not a convention: a page that claims the
    ;; governor refuses things must be produced by a run in which the
    ;; governor actually refused something.
    (when (zero? (count hard))
      (throw (ex-info "render-html: refusing to write a console from a run with ZERO :governor-hold records"
                      {:ledger-facts (count ledger) :governor-holds 0})))
    (when (< (count rules) 5)
      (throw (ex-info "render-html: refusing to write a console that does not exercise all five HARD governor rules"
                      {:rules-exercised rules :expected 5})))

    (let [html (render result)]
      (io/make-parents out)
      (spit out html)
      (println "wrote" out
               (str "(" (count html) " bytes, "
                    (count ledger) " ledger facts, "
                    (count hard) " governor holds, "
                    (count rules) " distinct HARD rules " (pr-str rules) ", "
                    (count (store/all-participants db)) " participants, "
                    (count (store/certification-history db)) " certification drafts, "
                    (count audit) " graph audit facts)")))))
