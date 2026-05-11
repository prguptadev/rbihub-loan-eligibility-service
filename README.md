# Loan Eligibility Service

A Spring Boot REST service that evaluates loan applications against a set
of business rules and returns either a single tenure-bound offer or a
structured rejection. Built for the RBIH Java Backend take-home.

## Tech stack

- Java 21
- Spring Boot 3.3.x (Web, Validation, Test starters)
- JUnit 5, AssertJ, Spring MockMvc
- Maven

No database - decisions are kept in an in-memory store behind a
repository interface so it can be swapped for a real datastore later.

## Build and run

```bash
mvn clean verify        # compile + run all tests
mvn spring-boot:run     # boot the service on :8080
# or
java -jar target/loan-eligibility-service-0.1.0-SNAPSHOT.jar
```

## API

The PDF specifies a single endpoint: `POST /applications`.

### `POST /applications`

Evaluate a single loan application.

**Request:**

```json
{
  "applicant": {
    "name": "Asha",
    "age": 30,
    "monthlyIncome": 75000,
    "employmentType": "SALARIED",
    "creditScore": 720
  },
  "loan": {
    "amount": 500000,
    "tenureMonths": 36,
    "purpose": "PERSONAL"
  }
}
```

**Approved (`200 OK`):**

```json
{
  "applicationId": "5bdcd604-080f-4262-8159-b5ce4809116d",
  "status": "APPROVED",
  "riskBand": "MEDIUM",
  "offer": {
    "interestRate": 13.50,
    "tenureMonths": 36,
    "emi": 16967.64,
    "totalPayable": 610835.04
  }
}
```

**Business rejection (`200 OK`):**

```json
{
  "applicationId": "a8b5503f-2c12-46a6-8f22-d43ef405f1c0",
  "status": "REJECTED",
  "riskBand": null,
  "rejectionReasons": [
    "CREDIT_SCORE_BELOW_MINIMUM",
    "AGE_TENURE_LIMIT_EXCEEDED"
  ]
}
```

A rejection is still a *successful evaluation* - the audit record is
saved and the response is `200 OK`. Only structurally invalid requests
return `400`.

Decisions are persisted to an in-memory audit store via
`DecisionRepository`. The PDF only requires that decisions be stored;
exposing them via a GET endpoint is intentionally out of scope.

**Validation failure (`400 Bad Request`):**

```json
{
  "timestamp": "2026-05-09T12:12:31.072+05:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/applications",
  "fieldErrors": [
    {"field": "applicant.age", "message": "age must be at least 21", "rejectedValue": 18}
  ]
}
```

## Business rules at a glance

| Gate                | Rule                                                                 |
|---------------------|----------------------------------------------------------------------|
| Credit score        | Must be `>= 600`                                                     |
| Age + tenure        | `age + tenureMonths` must keep the applicant `<= 65` at maturity     |
| EMI / income (hard) | EMI must be `<= 60%` of monthly income                               |
| EMI / income (offer)| Offer is only extended if EMI is `<= 50%` of monthly income          |

| Credit score | Risk band |
|--------------|-----------|
| `750+`       | `LOW`     |
| `650 - 749`  | `MEDIUM`  |
| `600 - 649`  | `HIGH`    |

Final annual rate = base 12% + risk premium + employment premium + loan-size premium.

## Project layout

```
src/main/java/in/rbihub/lending
├── LoanEligibilityServiceApplication.java
├── api/                  REST controllers, DTOs, exception handler
├── domain/               immutable value objects + sealed Decision hierarchy
├── repository/           audit-trail abstraction (in-memory impl)
└── service/              calculators, classifier, evaluator, orchestrator
```

## Tests

```bash
mvn test
```

The suite covers the EMI formula (boundary values, zero-rate edge case,
input guards), risk-band boundaries, every interest-premium combination,
each eligibility rule independently and combined, the full
orchestrator pipeline, and the controller through MockMvc.

