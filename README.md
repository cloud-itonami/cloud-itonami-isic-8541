# cloud-itonami-isic-8541

Open Business Blueprint for **ISIC Rev.5 8541**: Sports and recreation
education.

This repository publishes a sports-and-recreation-education actor --
participant intake, sports-instruction regulatory assessment,
coaching-staff-background-check screening and certification
finalization -- as an OSS business that any qualified, licensed
educator can fork, deploy, run, improve and sell, so a community or
independent academy never surrenders student data and ledgers to a
closed SaaS.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320),
[`7210`](https://github.com/cloud-itonami/cloud-itonami-isic-7210),
[`7410`](https://github.com/cloud-itonami/cloud-itonami-isic-7410),
[`8710`](https://github.com/cloud-itonami/cloud-itonami-isic-8710)) --
here it is **CoachOps-LLM ⊣ Instruction Safety Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a
> participant-intake summary, normalizing records, and checking
> whether a participant's own recorded attendance hours actually stay
> above their own recorded minimum requirement -- but it has **no
> notion of which jurisdiction's sports-instruction law is official,
> no license to finalize a real certification or safety-relevant
> progress record, and no way to know on its own whether a coaching-
> staff background check has actually stayed cleared**. Letting it
> finalize a certification directly invites fabricated regulatory
> citations, an under-hours participant being certified anyway, and an
> uncleared background check being quietly overlooked -- and
> liability, and participant-safety risk, for whoever runs it. This
> project seals the CoachOps-LLM into a single node and wraps it with
> an independent **Instruction Safety Governor**, a human **approval
> workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers participant intake through sports-instruction
regulatory assessment, coaching-staff-background-check screening and
certification finalization. It does **not**, by itself, hold any
license required to operate as a sports-instruction academy in a
given jurisdiction, and it does not claim to. It also does not model
a real academy-management system, the actual physical instruction
itself, or coaching/pedagogical judgment -- `sports.registry/
attendance-hours-insufficient?` is a pure ground-truth floor recompute
against the participant's own recorded fields, not a skill assessment.
Whoever deploys and operates a live instance (a licensed sports-
instruction academy) supplies any jurisdiction-specific license, the
real coaching workforce and the real academy-management-system
integrations, and bears that jurisdiction's liability -- the software
supplies the governed, spec-cited, audited execution scaffold so that
academy does not have to build the compliance layer from scratch.

### Actuation

**Finalizing a real certification or safety-relevant progress record
is never autonomous, at any phase, by construction.** Two independent
layers enforce this (`sports.governor`'s `:actuation/finalize-
certification` high-stakes gate and `sports.phase`'s phase table,
which never puts `:actuation/finalize-certification` in any phase's
`:auto` set) -- see `sports.phase`'s docstring and `test/sports/
phase_test.clj`'s `finalize-certification-never-auto-at-any-phase`.
The actor may draft, check and recommend; a human licensed educator is
always the one who actually finalizes a certification. Matching
`leasing`'s/`underwriting`'s/`testlab`'s/`clinic`'s/`veterinary`'s/
`funeral`'s/`parksafety`'s/`salon`'s/`entertainment`'s/`facility`'s/
`consulting`'s/`advertising`'s/`polling`'s/`research`'s/`design`'s
single-actuation shape, grounded directly in this blueprint's own
README text ("No automated proposal, by itself, can complete the
following without governor approval and audit evidence: finalizing a
certification or safety-relevant progress record") -- a POSITIVE
actuation (finalizing a real record), matching this fleet's majority
actuation shape (`3600`/`6190` are the fleet's two NEGATIVE-actuation
exceptions).

## The core contract

```
participant intake + jurisdiction facts (sports.facts, spec-cited)
        |
        v
   ┌──────────────┐   proposal      ┌───────────────────────┐
   │ CoachOps-LLM │ ─────────────▶ │ Instruction Safety            │  (independent system)
   │ (sealed)     │  + citations    │ Governor:                    │
   └──────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · attendance-       │
          │                         │ hours-insufficient (floor) ·    │
    record + ledger        escalate ┼ background-check-not-cleared     │
          │              (ALWAYS for│ (unconditional) ·                 │
          │               :actuation│ already-certified                   │
          │               /finalize-└───────────────────────┘
          ▼               certification)
      human approval
```

**The CoachOps-LLM never finalizes a certification the Instruction
Safety Governor would reject, and never does so without a human
sign-off.** Hard violations (fabricated regulatory requirements;
unsupported evidence; attendance hours below the participant's own
recorded minimum; an uncleared background check; a double
certification) force **hold** and *cannot* be approved past; a clean
finalization proposal still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a facility-safety monitoring
robot supports physical supervision during activities, under the
actor, gated by the independent **Instruction Safety Governor**. The
governor never dispatches hardware itself; `:high`/`:safety-critical`
actions require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Instruction Safety Governor, certification-finalization draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8541`). This vertical's academic/case records are practice-specific
rather than a shared cross-operator data contract, so `sports.*` runs
on the generic robotics/identity/forms/dmn/bpmn/audit-ledger stack
only -- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/sports/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + certification-finalization history. No dynamically-filed sub-record -- the actuation op acts directly on a pre-seeded participant, and the double-actuation guard checks a dedicated `:certified?` boolean rather than a `:status` value |
| `src/sports/registry.cljc` | Certification-finalization draft records, plus `attendance-hours-insufficient?` -- a literal reuse of `secondary.registry/attendance-hours-insufficient?`'s exact concept and field names, the EIGHTH instance of this fleet's MINIMUM-threshold sufficiency check family |
| `src/sports/facts.cljc` | Per-jurisdiction sports-instruction catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/sports/sportsadvisor.cljc` | **CoachOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/program-verification/background-check-screening/certification-finalization proposals |
| `src/sports/governor.cljc` | **Instruction Safety Governor** -- 3 HARD checks (spec-basis · evidence-incomplete · attendance-hours-insufficient, ground-truth floor recompute · background-check-not-cleared, unconditional evaluation, a literal reuse of `school.governor/background-check-not-cleared-violations`, the 43rd grounding of this discipline overall) + already-certified guard + 1 soft (confidence/actuation gate) |
| `src/sports/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (certification finalization always human; participant intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/sports/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/sports/sim.cljc` | demo driver |
| `test/sports/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers participant intake through sports-instruction
regulatory assessment, coaching-staff-background-check screening and
certification finalization -- the core governed lifecycle this
blueprint's own `docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Participant intake + per-jurisdiction sports-instruction checklisting, HARD-gated on an official spec-basis citation (`:participant/intake`/`:program/verify`) | Real academy-management-system integration, real physical instruction itself (see `sports.facts`'s docstring) |
| Coaching-staff-background-check screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:background-check/screen`) | Any coaching/pedagogical judgment itself -- deliberately outside this actor's competence |
| Certification finalization, HARD-gated on full evidence, the participant's own attendance-hours floor and a cleared background check, plus a double-certification guard (`:actuation/finalize-certification`) | |
| Immutable audit ledger for every intake/verification/screening/certification decision | |

Extending coverage is additive: add the next gate (e.g. a skill-
assessment-rubric check) as its own governed op with its own HARD
checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world act"
pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`sports.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `sports.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `sports.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `CoachOps-LLM` + `Instruction Safety Governor` run
as real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, modeled closely on the fifty-
eight prior actors' architecture. See `docs/adr/0001-architecture.md`
for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
