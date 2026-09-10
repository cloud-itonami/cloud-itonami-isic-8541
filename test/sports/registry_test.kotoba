(ns sports.registry-test
  (:require [clojure.test :refer [deftest is]]
            [sports.registry :as r]))

;; ----------------------------- attendance-hours-insufficient? -----------------------------

(deftest not-insufficient-when-hours-meet-minimum
  (is (not (r/attendance-hours-insufficient? {:attendance-hours-completed 40 :attendance-hours-required 30})))
  (is (not (r/attendance-hours-insufficient? {:attendance-hours-completed 30 :attendance-hours-required 30}))))

(deftest insufficient-when-hours-fall-short
  (is (r/attendance-hours-insufficient? {:attendance-hours-completed 15 :attendance-hours-required 30}))
  (is (r/attendance-hours-insufficient? {:attendance-hours-completed 29 :attendance-hours-required 30})))

(deftest insufficient-is-false-on-missing-fields
  (is (not (r/attendance-hours-insufficient? {})))
  (is (not (r/attendance-hours-insufficient? {:attendance-hours-completed 15}))))

;; ----------------------------- register-certification-finalization -----------------------------

(deftest certification-is-a-draft-not-a-real-certification
  (let [result (r/register-certification-finalization "participant-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest certification-assigns-certification-number
  (let [result (r/register-certification-finalization "participant-1" "JPN" 7)]
    (is (= (get result "certification_number") "JPN-CRT-000007"))
    (is (= (get-in result ["record" "participant_id"]) "participant-1"))
    (is (= (get-in result ["record" "kind"]) "certification-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest certification-validation-rules
  (is (thrown? Exception (r/register-certification-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-certification-finalization "participant-1" "" 0)))
  (is (thrown? Exception (r/register-certification-finalization "participant-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-certification-finalization "participant-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-certification-finalization "participant-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-CRT-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-CRT-000001" (get-in hist2 [1 "record_id"])))))
