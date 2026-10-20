# Job Hunter — automated internship discovery (Spring Boot + Apify)

Finds Java / Spring Boot internships and fresher roles in Pune and remote, from **LinkedIn, Internshala and Naukri**,
filters out senior roles, scams and unwanted locations, scores each posting against my resume, and stores
everything in PostgreSQL with a CSV export.

```
Apify actors (LinkedIn · Internshala · Naukri)
        │  run-sync-get-dataset-items
        ▼
 ApifyItemMapper ──► JobFilter ──► de-dup (URL + fingerprint) ──► MatchScorer ──► PostgreSQL
                     senior/2+ yrs/scam      no repeats             0–100 score      job_posting
                                                                                     discovery_run (audit log)
```

## Stack
Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 16 · Docker Compose · JUnit 5 · Apify API

---

## 1. One-time setup

1. **Apify account** → sign up at https://apify.com (free plan includes monthly platform credit).
2. **Token** → Apify Console → Settings → Integrations → copy your API token.
3. **Actor input fields** are already verified against `harvestapi/linkedin-job-search`
   (`jobTitles`, `locations`, `maxItems`, `postedLimit`, `experienceLevel`). If you switch to a different
   actor, edit `input-template` in `src/main/resources/application.yml`. Placeholders: `{keyword}` `{location}` `{limit}`.
4. **Env file**
   ```bash
   cp .env.example .env      # then paste your APIFY_TOKEN into .env
   ```
5. **Database** (start Docker Desktop first; uses port **5433** because a local PostgreSQL already has 5432)
   ```bash
   docker compose up -d
   ```

## 2. Run

```bash
mvnw test                 # unit tests (no network, no token needed) — no Maven install required
mvnw spring-boot:run      # app on http://localhost:8085 (only reachable from this PC)
```
(IntelliJ: just run `JobHunterApplication` — the `.env` file is picked up automatically.)

## 3. First run (manual — check cost before automating)

```bash
curl http://localhost:8085/api/discovery/status        # apifyTokenConfigured should be true
curl -X POST http://localhost:8085/api/discovery/run   # starts in background
curl http://localhost:8085/api/discovery/runs          # progress + per-search log
curl "http://localhost:8085/api/jobs?status=SHORTLISTED"
```
Open http://localhost:8085/api/jobs/export.csv?status=SHORTLISTED in a browser to download an Excel-ready CSV.

Then open Apify Console → *Runs* and check what that run cost. Happy with results + cost?
Set `scheduler-enabled: true` in `application.yml` → runs every weekday at 9 AM IST.

## 4. API

| Method | Endpoint | What it does |
|---|---|---|
| POST | `/api/discovery/run` | Start a run now (async) |
| POST | `/api/discovery/pause` / `resume` | Stop / allow runs (also stops a run between searches) |
| GET | `/api/discovery/status` | Paused? running? scheduler? token set? |
| GET | `/api/discovery/runs` | Last 20 runs with counts and log |
| GET | `/api/jobs?status=&minScore=&q=` | List jobs, best score first |
| PATCH | `/api/jobs/{id}` | `{"status":"APPLIED","notes":"..."}` |
| GET | `/api/jobs/export.csv` | CSV download |

Statuses: `NEW, SHORTLISTED, APPLIED, INTERVIEW, OFFER, REJECTED, SKIPPED, CLOSED`

## 5. How the score works

`score = matched skill weight ÷ (matched + missing weight) × 100`, +10 if the posting is clearly intern/fresher, capped 0–100.
- **matched** = skills on my resume that the JD mentions (Java, Spring Boot, JPA, React…)
- **missing** = common skills the JD wants that I don't have yet (Kafka, AWS, Kubernetes…) — a learning signal
- Score ≥ `min-score-to-shortlist` (60) → `SHORTLISTED`

Edit `MatchScorer.defaultProfile()` when I learn a new skill (flip `have` to `true`).

## 6. Cost guard rails

- `max-items-per-search: 25` × `max-searches-per-run: 5` = **max 125 Apify items per run**
- LinkedIn actors on Apify are pay-per-result; check the current price on the actor page before enabling the scheduler.
- Scheduler is **off by default**.

## 7. Notes / limits

- Scraping LinkedIn is against LinkedIn's user agreement. This uses a third-party Apify actor on public job listings only — no logins, no profile scraping. Use at your own judgement and keep volumes low.
- Always apply through the company's official careers page; never pay any "registration" or "training" fee.
- Roadmap: email draft generation (Gmail drafts, approval-first) → follow-up reminders → React dashboard.
