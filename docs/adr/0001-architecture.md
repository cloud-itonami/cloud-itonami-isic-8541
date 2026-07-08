# ADR-0001: CoachOps-LLM ⊣ Instruction Safety Governor architecture

## Status

Accepted. `cloud-itonami-isic-8541` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-8541` publishes an OSS business blueprint for
sports and recreation education: instruction in athletics, fitness and
recreational skills. Like every prior actor in this fleet, the
blueprint alone is not an implementation: this ADR records the
governed-actor architecture that promotes it to real, tested code,
following the same langgraph-clj StateGraph + independent Governor +
Phase 0→3 rollout pattern established by `cloud-itonami-isic-6511`
(life insurance) and applied across fifty-eight prior siblings, most
recently `cloud-itonami-isic-8710` (residential nursing care
facilities).

## Decision

### Decision 1: single-actuation shape

This blueprint's own README, business-model.md and operator-guide.md
consistently name only ONE real-world act: "finalizing a certification
or safety-relevant progress record." Matching `leasing`/
`underwriting`/`testlab`/`clinic`/`veterinary`/`funeral`/`parksafety`/
`salon`/`entertainment`/`facility`/`consulting`/`advertising`/
`polling`/`research`/`design`'s single-actuation shape, `high-stakes`
here is a one-member set, `#{:actuation/finalize-certification}`.

### Decision 2: entity and op shape

The primary entity is a `participant`. Four ops: `:participant/intake`
(directory upsert, no capital risk), `:program/verify` (per-
jurisdiction sports-instruction evidence checklist, never auto),
`:background-check/screen` (coaching-staff-background-check
screening, unconditional-evaluation discipline, never auto), and
`:actuation/finalize-certification` (POSITIVE, high-stakes --
finalizing a real certification or safety-relevant progress record).

### Decision 3: `attendance-hours-insufficient?` -- literal reuse for the 8th MINIMUM-threshold instance

Following `veterinary.registry/withdrawal-period-insufficient?` (1st,
temporal), `funeral.registry/waiting-period-elapsed?` (2nd, temporal),
`hospital.registry/observation-period-elapsed?` (3rd, temporal),
`association.registry/continuing-education-hours-insufficient?` (4th,
non-temporal), `secondary.registry/attendance-hours-insufficient?`
(5th, non-temporal), `polling.registry/sample-size-insufficient?` (6th,
non-temporal) and `research.registry/replication-count-insufficient?`
(7th, non-temporal), `sports.registry/attendance-hours-insufficient?`
LITERALLY reuses `secondary.registry/attendance-hours-insufficient?`'s
exact concept and field names (`:attendance-hours-completed`/
`:attendance-hours-required`) -- the SAME real-world requirement
(minimum instructional hours before a final credential) genuinely
recurs in this vertical, since ISIC `8541` is itself an education
class exactly like `secondary`/8521. The EIGHTH instance overall.
Gates only `:actuation/finalize-certification`.

### Decision 4: `background-check-not-cleared-violations` -- honestly reused (not new) as the 43rd unconditional-evaluation grounding

Before writing this check, `school.governor/background-check-not-
cleared-violations` was confirmed as the existing precedent (grep-
verified only one prior instance). `sports.governor/background-check-
not-cleared-violations` is a LITERAL reuse of the same concept -- a
youth-serving coaching-staff background/child-safety check, distinct
from `credential-not-current` (professional license currency, already
reused ~17 times across the fleet, most recently `nursing.governor`'s
42nd grounding). Not claimed as new; the SECOND instance of this
specific concept, and the 43rd distinct application of the
unconditional-evaluation discipline overall. Grounded directly in this
blueprint's own operator-guide text "finalizing a certification or
safety-relevant progress record always requires a human sign-off" and
Trust Control text implying background-check clearance for youth-
facing instruction. Gates `:background-check/screen` and
`:actuation/finalize-certification`.

### Decision 5: dedicated double-actuation-guard boolean

`:certified?` is a dedicated boolean on the `participant` record,
never a single `:status` value -- the same discipline every prior
sibling governor's guards establish, informed by `cloud-itonami-isic-
6492`'s real status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`sports.store/Store` is implemented by both `MemStore` (atom-backed,
default for dev/tests/demo) and `DatomicStore` (`langchain.db`-
backed), proven to satisfy the same contract in `test/sports/
store_contract_test.clj` -- the same seam every sibling actor uses so
swapping the SSoT backend is a configuration change, not a rewrite.
The protocol's per-entity accessor is named `participant` directly --
not a Clojure special form, so no `-of` suffix workaround was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:participant/intake`
(no capital risk). `:program/verify` and `:background-check/screen`
are never auto-eligible at any phase (matching every sibling's
screening-op posture), and `:actuation/finalize-certification` is
permanently excluded from every phase's `:auto` set -- a structural
fact, not a rollout milestone, enforced by BOTH `sports.phase` and
`sports.governor`'s `high-stakes` set independently.

### Decision 8: no bespoke domain capability lib as a code dependency

This blueprint's own `:itonami.blueprint/required-technologies` names
no domain-specific capability beyond the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack -- there was no capability-lib
decision to make at all.

### Decision 9: mock + LLM advisor pair

`sports.sportsadvisor` provides `mock-advisor` (deterministic, default
everywhere -- the actor graph and governor contract run offline) and
`llm-advisor` (backed by `langchain.model/ChatModel`, with a defensive
EDN-proposal parser so a malformed LLM response degrades to a safe
low-confidence noop rather than ever auto-finalizing a certification).

### Decision 10: no `blueprint.edn` field-sync fixes needed

Matching `advertising`/7310's, `polling`/7320's, `research`/7210's,
`design`/7410's and `nursing`/8710's own experience, this repo's
`blueprint.edn` already had the correct `isic-` prefixed `:id` and
correctly populated `:required-technologies`/`:optional-technologies`
matching the `kotoba-lang/industry` registry's own entry for `"8541"`
exactly -- only the `:maturity` field itself needed adding.

## Alternatives considered

- **A dual-actuation shape** (e.g. splitting "finalizing a
  certification" and "finalizing a safety-relevant progress record"
  into two separate actuations). Rejected: the blueprint's own text
  treats these as ONE act ("finalizing A certification OR safety-
  relevant progress record" -- an either/or phrasing of the SAME kind
  of act on the same record, not two distinct real-world acts);
  inventing a second actuation would not be grounded in the
  blueprint's own text.
- **Inventing a new name for the attendance-hours concept** (e.g.
  "training-hours-insufficient"). Rejected: this is genuinely the SAME
  real-world requirement already established by `secondary.registry/
  attendance-hours-insufficient?` for the same education-class family
  -- reusing the literal name is more honest than inventing a
  cosmetically-different one.
- **Reusing `credential-not-current` instead of `background-check-not-
  cleared` for the coaching-staff screening concept.** Rejected: these
  are genuinely different real-world concerns (professional license
  currency vs. youth-safety background/criminal-history clearance);
  `school.governor`'s existing `background-check-not-cleared` concept
  is the more precise, already-established match for a youth-serving
  instruction context.

## Consequences

- Fifty-ninth actor in this fleet (58 implemented before this build).
- Confirms the MINIMUM-threshold sufficiency check family generalizes
  to an 8th instance via honest literal reuse (not a novel concept).
- Confirms `background-check-not-cleared` generalizes to a 2nd literal
  instance, continuing the unconditional-evaluation discipline's count
  to 43.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/sports/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself.

## References

- `orgs/cloud-itonami/cloud-itonami-isic-8541/README.md`
- `orgs/cloud-itonami/cloud-itonami-isic-8541/docs/business-model.md`
- `orgs/kotoba-lang/industry/resources/kotoba/industry/registry.edn` (entry `"8541"`)
