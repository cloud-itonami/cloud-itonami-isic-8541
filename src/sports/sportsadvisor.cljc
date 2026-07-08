(ns sports.sportsadvisor
  "CoachOps-LLM client -- the *contained intelligence node* for the
  sports-and-recreation-education actor.

  It normalizes participant intake, drafts a per-jurisdiction sports-
  instruction evidence checklist, screens participants for an
  uncleared coaching-staff background check, and drafts the
  certification-finalization action. CRITICAL: it is a smart-but-
  untrusted advisor. It returns a *proposal* (with a rationale + the
  fields it cited), never a committed record or a real certification
  finalization. Every output is censored downstream by `sports.
  governor` before anything touches the SSoT, and `:actuation/
  finalize-certification` proposals NEVER auto-commit at any phase --
  see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/finalize-certification | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [sports.facts :as facts]
            [sports.registry :as registry]
            [sports.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the participant, program or jurisdiction. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "参加者記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :participant/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-program
  "Per-jurisdiction sports-instruction evidence checklist draft.
  `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a jurisdiction with NO official spec-basis
  in `sports.facts` -- the Instruction Safety Governor must reject
  this (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [p (store/participant db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction p))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "sports.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :program/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :program/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-background-check
  "Coaching-staff background-check screening draft.
  `:background-check-cleared?` on the participant record injects the
  failure mode: the Instruction Safety Governor must HOLD, un-
  overridably, on any uncleared check."
  [db {:keys [subject]}]
  (let [p (store/participant db subject)]
    (cond
      (nil? p)
      {:summary "対象参加者記録が見つかりません" :rationale "no participant record"
       :cites [] :effect :background-check/set :value {:participant-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (false? (:background-check-cleared? p))
      {:summary    (str (:participant-name p) ": 指導者の身元確認が未完了")
       :rationale  "スクリーニングが身元確認未完了を検出。人手確認とホールドが必須。"
       :cites      [:background-check]
       :effect     :background-check/set
       :value      {:participant-id subject :verdict :not-cleared}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:participant-name p) ": 身元確認は完了")
       :rationale  "身元確認スクリーニング完了。"
       :cites      [:background-check]
       :effect     :background-check/set
       :value      {:participant-id subject :verdict :cleared}
       :stake      nil
       :confidence 0.9})))

(defn- propose-certification-finalization
  "Draft the actual CERTIFICATION-FINALIZATION action -- finalizing a
  real certification or safety-relevant progress record. ALWAYS
  `:stake :actuation/finalize-certification` -- this is a REAL-WORLD
  act, never a draft the actor may auto-run. See README `Actuation`:
  no phase ever adds this op to a phase's `:auto` set (`sports.
  phase`); the governor also always escalates on `:actuation/
  finalize-certification`. Two independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [p (store/participant db subject)
        safe? (and p (not (registry/attendance-hours-insufficient? p))
                   (not (false? (:background-check-cleared? p))))]
    {:summary    (str subject " 向け資格確定提案"
                      (when p (str " (participant=" (:participant-name p) ")")))
     :rationale  (if p
                   (str "attendance-hours-completed=" (:attendance-hours-completed p)
                        " attendance-hours-required=" (:attendance-hours-required p))
                   "参加者記録が見つかりません")
     :cites      (if p [subject] [])
     :effect     :participant/mark-certified
     :value      {:participant-id subject}
     :stake      :actuation/finalize-certification
     :confidence (if safe? 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :participant/intake                (normalize-intake db request)
    :program/verify                    (verify-program db request)
    :background-check/screen           (screen-background-check db request)
    :actuation/finalize-certification  (propose-certification-finalization db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたはスポーツ・レクリエーション教育事業の資格確定エージェントの"
       "助言者です。与えられた事実のみに基づき、提案を1つだけEDNマップで"
       "返します。説明や前置きは一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:participant/upsert|:program/set|:background-check/set|"
       ":participant/mark-certified) "
       ":stake(:actuation/finalize-certification か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :program/verify                    {:participant (store/participant st subject)}
    :background-check/screen           {:participant (store/participant st subject)}
    :actuation/finalize-certification  {:participant (store/participant st subject)}
    {:participant (store/participant st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Instruction Safety Governor
  escalates/holds -- an LLM hiccup can never auto-finalize a
  certification."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :sportsadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
