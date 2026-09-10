(ns sports.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [sports.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Kenji" (:participant-name (store/participant s "participant-1"))))
      (is (= "JPN" (:jurisdiction (store/participant s "participant-1"))))
      (is (= 40 (:attendance-hours-completed (store/participant s "participant-1"))))
      (is (= 30 (:attendance-hours-required (store/participant s "participant-1"))))
      (is (true? (:background-check-cleared? (store/participant s "participant-1"))))
      (is (= 15 (:attendance-hours-completed (store/participant s "participant-3"))))
      (is (false? (:background-check-cleared? (store/participant s "participant-4"))))
      (is (false? (:certified? (store/participant s "participant-1"))))
      (is (= ["participant-1" "participant-2" "participant-3" "participant-4"]
             (mapv :id (store/all-participants s))))
      (is (nil? (store/background-check-of s "participant-1")))
      (is (nil? (store/program-of s "participant-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/certification-history s)))
      (is (zero? (store/next-certification-sequence s "JPN")))
      (is (false? (store/participant-already-certified? s "participant-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :participant/upsert
                                 :value {:id "participant-1" :participant-name "Sato Kenji"}})
        (is (= "Sato Kenji" (:participant-name (store/participant s "participant-1"))))
        (is (= 30 (:attendance-hours-required (store/participant s "participant-1"))) "unrelated field preserved"))
      (testing "program / background-check payloads commit and read back"
        (store/commit-record! s {:effect :program/set :path ["participant-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/program-of s "participant-1")))
        (store/commit-record! s {:effect :background-check/set :path ["participant-1"]
                                 :payload {:participant-id "participant-1" :verdict :cleared}})
        (is (= {:participant-id "participant-1" :verdict :cleared} (store/background-check-of s "participant-1"))))
      (testing "certification finalization drafts a record and advances the sequence"
        (store/commit-record! s {:effect :participant/mark-certified :path ["participant-1"]})
        (is (= "JPN-CRT-000000" (get (first (store/certification-history s)) "record_id")))
        (is (= "certification-finalization-draft" (get (first (store/certification-history s)) "kind")))
        (is (true? (:certified? (store/participant s "participant-1"))))
        (is (= 1 (count (store/certification-history s))))
        (is (= 1 (store/next-certification-sequence s "JPN")))
        (is (true? (store/participant-already-certified? s "participant-1")))
        (is (false? (store/participant-already-certified? s "participant-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/participant s "nope")))
    (is (= [] (store/all-participants s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/certification-history s)))
    (is (zero? (store/next-certification-sequence s "JPN")))
    (store/with-participants s {"x" {:id "x" :participant-name "n"
                                     :attendance-hours-completed 40 :attendance-hours-required 30
                                     :background-check-cleared? true
                                     :certified? false :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:participant-name (store/participant s "x"))))))
