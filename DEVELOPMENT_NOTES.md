# Development notes

## Overall approach

Read the spec twice, listed the moving parts on paper: validation,
risk band, rate composition, EMI, eligibility, offer generation,
audit. Each one is its own concern so each got its own class in the
service layer. Felt cleaner than a single `RuleEngine` class doing
everything. The orchestrator (`LoanApplicationService`) just walks
the pipeline; it doesn't carry any rules of its own.

Built it bottom-up. Domain types first, then each service component
with tests, then the API on top. That order meant I could test every
piece in isolation before wiring it together.

## Key design decisions

**Sealed `Decision` hierarchy.** A decision can only end in approved
or rejected, nothing else. Modelling that as a sealed interface with
two record variants (`Approved`, `Rejected`) means the controller can
switch over them exhaustively — if anyone adds a third state later,
the compiler will flag every switch that doesn't handle it. Also
keeps the fields clean: `Approved` has an offer, `Rejected` has
reasons, no nullable offer-or-reasons floating around in one big DTO.

**`Optional<RiskBand>` for sub-600 scores.** The risk-band table
starts at 600. Returning an empty `Optional` for anything below it,
rather than inventing some `UNRATED` value, makes the "no band, no
rate, no EMI" path explicit. The orchestrator has to consciously
handle that case.

**BigDecimal everywhere financial.** EMI compounds at
`MathContext(20, HALF_UP)` (generous headroom for 360-month tenures),
then the final EMI is rounded to 2 decimals with `HALF_UP`, like the
spec asks. The 0% rate edge case is short-circuited to `principal / n`
so we don't divide by zero.

**Two EMI gates, two rejection reasons.** The spec mentions both a
60% hard cap (eligibility) and a 50% offer-validity cap. I kept them
as separate codes (`EMI_EXCEEDS_60_PERCENT`, `EMI_EXCEEDS_50_PERCENT`)
so the audit trail tells you which gate caught it. In practice the
50% cap is the binding one, but the spec explicitly shows the 60%
code in the example response so I went with both.

**Eligibility collects, doesn't short-circuit.** The spec's rejection
example shows two reasons in one response, so the evaluator runs every
rule and returns the full list. That way the applicant gets the full
picture in one go instead of fixing one thing and getting rejected
again for another.

**Records for value objects and DTOs.** Immutable, no Lombok needed.
Compact constructors do `Objects.requireNonNull` for reference fields.
The `Decision.Rejected` record defensively copies its reasons list so
the audit record can't be tampered with later.

**Mapper as a bean.** `ApplicationDtoMapper` is a `@Component` rather
than a static helper class — keeps Jackson and bean-validation
annotations on the DTO side, and the domain types stay framework-free.

**Audit store behind an interface.** `DecisionRepository` is the
abstraction; `InMemoryDecisionRepository` is the only implementation
today. Backed by `ConcurrentHashMap` since Tomcat will run requests
on multiple threads. Swapping for JPA later is a one-class change.

**`200 OK` for the POST.** The PDF asks for one endpoint
(`POST /applications`) and "stores decisions for audit" but *not* for
a retrieval endpoint. So the public API is just that one POST — it
returns the decision in the body and saves a copy through
`DecisionRepository.save` for audit. Both approved and rejected
outcomes come back as `200` since both are valid evaluation results.
`201` didn't fit because there's no resource URL to follow up with.
Only validation failures (where no audit record is created) come back
as `4xx`.

## Trade-offs considered

- **No Lombok.** Records cover most of what Lombok would generate,
  and skipping it keeps the dependencies short and the generated
  code visible to anyone reading.

- **`@SpringBootTest` over `@WebMvcTest` for the controller.** A
  slice test would boot faster, but the service has no external
  dependencies and the full context still comes up in under a second.
  Wiring the real beans gave me more confidence in the end-to-end
  JSON contract.

- **No persistence layer.** The spec asks for audit storage, not
  durability. An in-memory map is the smallest thing that satisfies
  it. The repository interface is there for when we need to swap it.

- **Value-based unit tests over property-based.** I went with
  parameterised value tests for the EMI calculator — they read
  clearly and pin down regressions on the exact paisa. Property-based
  tests (longer tenure → lower EMI, etc.) would catch a different
  class of bug; left as a follow-up.

- **`monthlyIncome` as `BigDecimal`.** Could've been a `long` for
  whole rupees, but every downstream calculation works on `BigDecimal`
  anyway. Standardising at the boundary avoids conversions and
  any precision surprises during the percentage checks.

## Assumptions

- Loan amount upper bound is 50,00,000 (50 lakh), interpreted from
  the spec's "10,000 - 50,00,000" wording.

- The age + tenure rule is "must not exceed 65 at maturity", strict
  greater-than fails. A 60-year-old with a 60-month loan reaches
  exactly 65 at the end and is accepted; 61 months would reject.
  I worked in months internally so the boundary is unambiguous.

- The 50% gate is the stricter rule and effectively the binding one.
  If EMI is over 60% the eligibility gate trips first; the 50% reason
  only shows up when EMI lands between 50% and 60%.

- Risk band on a rejection is null in the response (per the spec's
  example), even though it's computed internally where possible. We
  just don't surface it.

- `name` is treated as any non-blank string. No KYC-style validation
  — that should sit upstream of this service.

## Things I'd do with more time

- **Expose decision retrieval endpoints for the audit role.** Right
  now decisions are saved to the repository but there's no way to
  read them back over HTTP. A compliance officer or back-office user
  would have to query the DB. With more time:
  - `GET /applications/{id}` to fetch one decision by id (the
    `applicationId` is already in the POST response, so the client
    side is free)
  - `GET /applications` with paging and filters (date range, status,
    rejection reason) so an audit user can list decisions taken in
    the last 24 hours, or pull every rejection for
    `EMI_EXCEEDS_60_PERCENT` last quarter
  - Both endpoints behind role-based auth (`AUDITOR`, `BACK_OFFICE`)
    rather than on the public lending API

- **Move audit storage to a real database.** The in-memory store
  satisfies the spec but loses everything on restart. A JPA + Postgres
  implementation would persist:
  - Full request payload (so historical decisions can be re-checked
    if a rule changes)
  - The decision and its reasons / offer
  - Audit columns: `requested_at`, `decided_at`, `rule_policy_version`,
    `request_id` (the MDC id, for log correlation)
  Indexes on `decided_at` and `status` for the audit list query above.
  Repository interface stays the same, only the implementation class
  changes.

- **A simple back-office UI for the audit team.** Once the retrieval
  endpoints exist, a thin Thymeleaf table view (filters,
  click-into-detail, CSV export) lets non-technical compliance staff
  browse decisions themselves.

- **Versioned rule policy.** Now that thresholds live in
  `application.yml`, give the policy a `version` field and stamp it
  on every `Decision`. An audit query later can then ask "what rule
  set was this decision taken under?" without guessing.

- **OpenAPI / Swagger UI.** A few annotations and `springdoc-openapi`
  would publish a live API contract. Also makes the audit team's
  scripts easier to point.

- **Metrics with Micrometer.** Logging is in place (one INFO line per
  decision, DEBUG for rule traces, request id via MDC). What's still
  missing is counters by outcome and rejection reason so dashboards
  can show "rejection rate by reason over time" without parsing logs.

- **Idempotency.** Accept an `Idempotency-Key` header and dedupe
  retries — useful when this sits behind a flaky network or a mobile
  client.

- **Property-based tests** for the financial calculations (EMI is
  monotonic in rate, monotonic in principal, decreasing in tenure for
  positive rates).

- **i18n for error messages.** Bean-validation messages could move
  to a `ValidationMessages.properties` so they're swappable per
  locale.

- **A lightweight event stream** if downstream systems (CRM,
  notification services) ever need to react to decisions — publish
  an `ApplicationLifecycleEvent` from the orchestrator and let
  consumers subscribe.
