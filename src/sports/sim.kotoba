(ns sports.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean participant
  through intake -> program verification -> background-check
  screening -> certification-finalization proposal (always escalates)
  -> human approval -> commit, then shows four HARD holds (a
  jurisdiction with no spec-basis, a participant whose own attendance
  hours fall short of their own recorded minimum, an uncleared
  background check screened directly via `:background-check/screen`
  [never via an actuation op against an unscreened participant -- see
  this actor's own governor ns docstring / the lesson `parksafety`'s
  ADR-2607071922 Decision 5, `eldercare`'s, `museum`'s,
  `conservation`'s, `salon`'s, `entertainment`'s, `casework`'s,
  `hospital`'s, `facility`'s, `school`'s, `association`'s, `leasing`'s,
  `behavioral`'s, `secondary`'s, `card`'s, `water`'s, `telecom`'s,
  `aerospace`'s, `recovery`'s, `consulting`'s, `union`'s,
  `congregation`'s, `fab`'s, `energy`'s, `care`'s, `navigator`'s,
  `learning`'s, `banking`'s, `advertising`'s, `polling`'s, `research`'s,
  `design`'s and `nursing`'s ADR-0001s already recorded], and a double
  certification of an already-processed participant) that never reach
  a human at all, and prints the audit ledger + the draft
  certification-finalization records."
  (:require [langgraph.graph :as g]
            [sports.store :as store]
            [sports.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :licensed-educator :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== participant/intake participant-1 (JPN, clean; attendance sufficient, background cleared) ==")
    (println (exec! actor "t1" {:op :participant/intake :subject "participant-1"
                                :patch {:id "participant-1" :participant-name "Sato Kenji"}} operator))

    (println "== program/verify participant-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :program/verify :subject "participant-1"} operator))
    (println (approve! actor "t2"))

    (println "== background-check/screen participant-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :background-check/screen :subject "participant-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/finalize-certification participant-1 (always escalates -- actuation/finalize-certification) ==")
    (let [r (exec! actor "t4" {:op :actuation/finalize-certification :subject "participant-1"} operator)]
      (println r)
      (println "-- human licensed educator approves --")
      (println (approve! actor "t4")))

    (println "== program/verify participant-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t5" {:op :program/verify :subject "participant-2" :no-spec? true} operator))

    (println "== program/verify participant-3 (escalates -- human approves; sets up the attendance-hours test) ==")
    (println (exec! actor "t6" {:op :program/verify :subject "participant-3"} operator))
    (println (approve! actor "t6"))

    (println "== actuation/finalize-certification participant-3 (attendance 15h < required 30h -> HARD hold) ==")
    (println (exec! actor "t7" {:op :actuation/finalize-certification :subject "participant-3"} operator))

    (println "== background-check/screen participant-4 (not-cleared -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :background-check/screen :subject "participant-4"} operator))

    (println "== actuation/finalize-certification participant-1 AGAIN (double-certification -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/finalize-certification :subject "participant-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft certification-finalization records ==")
    (doseq [r (store/certification-history db)] (println r))))
