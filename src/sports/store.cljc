(ns sports.store
  "SSoT for the sports-and-recreation-education actor, behind a `Store`
  protocol so the backend is a swap, not a rewrite -- the same seam
  every prior `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/sports/store_contract_test.clj), which is the whole point: the
  actor, the Instruction Safety Governor and the audit ledger never
  know which SSoT they run on.

  Like `clinic.store`'s/`credit.store`'s/`accounting.store`'s simpler
  entities, a PARTICIPANT is acted on directly by the ONE actuation
  op -- no dynamically-filed sub-record, and the double-finalization
  guard checks a dedicated `:certified?` boolean rather than a
  `:status` value, the same discipline `clinic.governor`'s/
  `accounting.governor`'s/`marketadmin.governor`'s guards establish.

  NOTE on naming: the protocol's per-entity accessor is `participant`
  directly -- not a Clojure special form, so no `-of` suffix workaround
  was needed.

  The ledger stays append-only on every backend: 'which participant
  was screened for a cleared background check, which certification
  was finalized, on what jurisdictional basis, approved by whom' is
  always a query over an immutable log -- the audit trail a family
  trusting an academy needs, and the evidence an operator needs if a
  certification decision is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [sports.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (participant [s id])
  (all-participants [s])
  (background-check-of [s participant-id] "committed background-check screening verdict for a participant, or nil")
  (program-of [s participant-id] "committed program-curriculum evidence assessment, or nil")
  (ledger [s])
  (certification-history [s] "the append-only certification-finalization history (sports.registry drafts)")
  (next-certification-sequence [s jurisdiction] "next certification-number sequence for a jurisdiction")
  (participant-already-certified? [s participant-id] "has this participant already been certified?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-participants [s participants] "replace/seed the participant directory (map id->participant)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained participant set so the actor + tests run
  offline."
  []
  {:participants
   {"participant-1" {:id "participant-1" :participant-name "Sato Kenji"
                     :attendance-hours-completed 40 :attendance-hours-required 30
                     :background-check-cleared? true
                     :certified? false :jurisdiction "JPN" :status :intake}
    "participant-2" {:id "participant-2" :participant-name "Atlantis Doe"
                     :attendance-hours-completed 40 :attendance-hours-required 30
                     :background-check-cleared? true
                     :certified? false :jurisdiction "ATL" :status :intake}
    "participant-3" {:id "participant-3" :participant-name "鈴木花子"
                     :attendance-hours-completed 15 :attendance-hours-required 30
                     :background-check-cleared? true
                     :certified? false :jurisdiction "JPN" :status :intake}
    "participant-4" {:id "participant-4" :participant-name "田中一郎"
                     :attendance-hours-completed 40 :attendance-hours-required 30
                     :background-check-cleared? false
                     :certified? false :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- finalize-certification!
  "Backend-agnostic `:participant/mark-certified` -- looks up the
  participant via the protocol and drafts the certification-
  finalization record, and returns {:result .. :participant-patch ..}
  for the caller to persist."
  [s participant-id]
  (let [p (participant s participant-id)
        seq-n (next-certification-sequence s (:jurisdiction p))
        result (registry/register-certification-finalization participant-id (:jurisdiction p) seq-n)]
    {:result result
     :participant-patch {:certified? true
                         :certification-number (get result "certification_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (participant [_ id] (get-in @a [:participants id]))
  (all-participants [_] (sort-by :id (vals (:participants @a))))
  (background-check-of [_ id] (get-in @a [:background-checks id]))
  (program-of [_ participant-id] (get-in @a [:programs participant-id]))
  (ledger [_] (:ledger @a))
  (certification-history [_] (:certifications @a))
  (next-certification-sequence [_ jurisdiction] (get-in @a [:certification-sequences jurisdiction] 0))
  (participant-already-certified? [_ participant-id] (boolean (get-in @a [:participants participant-id :certified?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :participant/upsert
      (swap! a update-in [:participants (:id value)] merge value)

      :program/set
      (swap! a assoc-in [:programs (first path)] payload)

      :background-check/set
      (swap! a assoc-in [:background-checks (first path)] payload)

      :participant/mark-certified
      (let [participant-id (first path)
            {:keys [result participant-patch]} (finalize-certification! s participant-id)
            jurisdiction (:jurisdiction (participant s participant-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:certification-sequences jurisdiction] (fnil inc 0))
                       (update-in [:participants participant-id] merge participant-patch)
                       (update :certifications registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-participants [s participants] (when (seq participants) (swap! a assoc :participants participants)) s))

(defn seed-db
  "A MemStore seeded with the demo participant set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :programs {} :background-checks {} :ledger [] :certification-sequences {}
                           :certifications []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (program/background-check payloads, ledger facts,
  certification records) are stored as EDN strings so `langchain.db`
  doesn't expand them into sub-entities -- the same convention every
  sibling actor's store uses."
  {:participant/id                          {:db/unique :db.unique/identity}
   :program/participant-id                  {:db/unique :db.unique/identity}
   :background-check/participant-id         {:db/unique :db.unique/identity}
   :ledger/seq                              {:db/unique :db.unique/identity}
   :certification/seq                       {:db/unique :db.unique/identity}
   :certification-sequence/jurisdiction     {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- participant->tx [{:keys [id participant-name attendance-hours-completed attendance-hours-required
                                background-check-cleared? certified? jurisdiction status certification-number]}]
  (cond-> {:participant/id id}
    participant-name                     (assoc :participant/participant-name participant-name)
    attendance-hours-completed           (assoc :participant/attendance-hours-completed attendance-hours-completed)
    attendance-hours-required            (assoc :participant/attendance-hours-required attendance-hours-required)
    (some? background-check-cleared?)    (assoc :participant/background-check-cleared? background-check-cleared?)
    (some? certified?)                   (assoc :participant/certified? certified?)
    jurisdiction                          (assoc :participant/jurisdiction jurisdiction)
    status                               (assoc :participant/status status)
    certification-number                 (assoc :participant/certification-number certification-number)))

(def ^:private participant-pull
  [:participant/id :participant/participant-name :participant/attendance-hours-completed
   :participant/attendance-hours-required :participant/background-check-cleared? :participant/certified?
   :participant/jurisdiction :participant/status :participant/certification-number])

(defn- pull->participant [m]
  (when (:participant/id m)
    {:id (:participant/id m) :participant-name (:participant/participant-name m)
     :attendance-hours-completed (:participant/attendance-hours-completed m)
     :attendance-hours-required (:participant/attendance-hours-required m)
     :background-check-cleared? (boolean (:participant/background-check-cleared? m))
     :certified? (boolean (:participant/certified? m))
     :jurisdiction (:participant/jurisdiction m) :status (:participant/status m)
     :certification-number (:participant/certification-number m)}))

(defrecord DatomicStore [conn]
  Store
  (participant [_ id]
    (pull->participant (d/pull (d/db conn) participant-pull [:participant/id id])))
  (all-participants [_]
    (->> (d/q '[:find [?id ...] :where [?e :participant/id ?id]] (d/db conn))
         (map #(pull->participant (d/pull (d/db conn) participant-pull [:participant/id %])))
         (sort-by :id)))
  (background-check-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?pid
                :where [?k :background-check/participant-id ?pid] [?k :background-check/payload ?p]]
              (d/db conn) id)))
  (program-of [_ participant-id]
    (dec* (d/q '[:find ?p . :in $ ?pid
                :where [?a :program/participant-id ?pid] [?a :program/payload ?p]]
              (d/db conn) participant-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (certification-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :certification/seq ?s] [?e :certification/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-certification-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :certification-sequence/jurisdiction ?j] [?e :certification-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (participant-already-certified? [s participant-id]
    (boolean (:certified? (participant s participant-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :participant/upsert
      (d/transact! conn [(participant->tx value)])

      :program/set
      (d/transact! conn [{:program/participant-id (first path) :program/payload (enc payload)}])

      :background-check/set
      (d/transact! conn [{:background-check/participant-id (first path) :background-check/payload (enc payload)}])

      :participant/mark-certified
      (let [participant-id (first path)
            {:keys [result participant-patch]} (finalize-certification! s participant-id)
            jurisdiction (:jurisdiction (participant s participant-id))
            next-n (inc (next-certification-sequence s jurisdiction))]
        (d/transact! conn
                     [(participant->tx (assoc participant-patch :id participant-id))
                      {:certification-sequence/jurisdiction jurisdiction :certification-sequence/next next-n}
                      {:certification/seq (count (certification-history s)) :certification/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-participants [s participants]
    (when (seq participants) (d/transact! conn (mapv participant->tx (vals participants)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:participants ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [participants]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-participants s participants))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo participant set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
