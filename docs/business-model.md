# Business Model: Sports and recreation education

## Classification

- Repository: `cloud-itonami-isic-8541`
- ISIC Rev.5: `8541`
- Activity: sports and recreation education -- instruction in athletics, fitness and recreational skills
- Social impact: education access, data sovereignty, transparent audit

## Customer

- independent sports academies
- cooperative coaching collectives
- community recreation programs

## Offer

- participant enrollment intake
- program/curriculum proposal
- certification/progress proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per academy
- support: monthly retainer with SLA
- migration: import from an incumbent academy-management system
- per-enrollment fee

## Trust Controls

- no certification or safety-relevant progress record is finalized without human sign-off
- a fabricated assessment forces a hold, not an override
- every record path is auditable
- participant data stays outside Git
- emergency manual override paths remain outside LLM control
- attendance hours below a participant's own recorded minimum requirement,
  or an uncleared coaching-staff background check, forces a hold, not an
  override
- certification finalization is logged and escalated, and cannot be
  finalized twice for the same participant: a double-certification attempt
  is held off this actor's own participant facts alone, with no upstream
  comparison needed

## Instruction Safety Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:instruction-
safety-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (finalizing a
certification or safety-relevant progress record) must pass. The
governor sits between the CoachOps-LLM and execution, per the
README's Core Contract:

```text
CoachOps-LLM -> Instruction Safety Governor -> hold, proceed, or human approval
```

**Approves**: routine instruction actions proposed against a
participant who already has a consented program on file, attendance
hours at or above their own recorded minimum, and a cleared coaching-
staff background check. These proceed straight to the academic
ledger.

**Rejects or escalates**: the governor refuses to let the advisor
finalize a certification on its own authority when any of the
following hold -- a fabricated jurisdiction spec-basis; incomplete
evidence; attendance hours below the participant's own recorded
minimum; an uncleared background check. A clean finalization proposal
still always routes to a human -- `:actuation/finalize-certification`
is never auto-committed, at any rollout phase.
