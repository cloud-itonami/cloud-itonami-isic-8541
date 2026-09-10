(require '[sports.store :as store]
         '[sports.operation :as op]
         '[langgraph.graph :as g]
         '[clojure.pprint :as pp])

(def operator {:actor-id "op-1" :actor-role :licensed-educator :phase 3})
(defn ex! [a tid req ctx] (g/run* a {:request req :context ctx} {:thread-id tid}))
(defn ap! [a tid] (g/run* a {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))
(defn rj! [a tid] (g/run* a {:approval {:status :rejected :by "op-1"}} {:thread-id tid :resume? true}))

(let [db (store/seed-db) a (op/build db)]
  (ex! a "t1a" {:op :participant/intake :subject "participant-1"
                :patch {:id "participant-1" :participant-name "Sato Kenji"}} operator)
  (ex! a "t1b" {:op :program/verify :subject "participant-1"} operator) (ap! a "t1b")
  (ex! a "t1c" {:op :background-check/screen :subject "participant-1"} operator) (ap! a "t1c")
  (ex! a "t1d" {:op :actuation/finalize-certification :subject "participant-1"} operator) (ap! a "t1d")
  (ex! a "t1e" {:op :actuation/finalize-certification :subject "participant-1"} operator)
  (ex! a "t2a" {:op :program/verify :subject "participant-2" :no-spec? true} operator)
  (println ">>> t2b finalize participant-2 (no program on file):")
  (pp/pprint (:disposition (:state (ex! a "t2b" {:op :actuation/finalize-certification :subject "participant-2"} operator))))
  (ex! a "t3a" {:op :program/verify :subject "participant-3"} operator) (ap! a "t3a")
  (ex! a "t3b" {:op :actuation/finalize-certification :subject "participant-3"} operator)
  (ex! a "t4a" {:op :background-check/screen :subject "participant-4"} operator)
  (ex! a "t4b" {:op :program/verify :subject "participant-4"} operator) (ap! a "t4b")
  (println ">>> t4c finalize participant-4 (bg NOT cleared, no committed bg record):")
  (let [r (ex! a "t4c" {:op :actuation/finalize-certification :subject "participant-4"} operator)]
    (pp/pprint (select-keys (:state r) [:disposition :verdict])))
  (rj! a "t4c")
  (println ">>> phase-0 intake participant-2:")
  (pp/pprint (:disposition (:state (ex! a "t0" {:op :participant/intake :subject "participant-2" :patch {:id "participant-2"}}
                                        (assoc operator :phase 0)))))

  (println "\n===== programs register =====")
  (doseq [pid ["participant-1" "participant-3" "participant-4"]]
    (println pid "->") (pp/pprint (store/program-of db pid)))
  (println "\n===== background-checks register =====")
  (doseq [pid ["participant-1"]]
    (println pid "->") (pp/pprint (store/background-check-of db pid)))
  (println "\n===== participants =====")
  (doseq [p (store/all-participants db)] (pp/pprint p))
  (println "\n===== certification history =====")
  (doseq [r (store/certification-history db)] (pp/pprint r))
  (println "\n===== ledger =====")
  (doseq [f (store/ledger db)] (pp/pprint f)))
