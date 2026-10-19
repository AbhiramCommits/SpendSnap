# SpendSnap

SpendSnap is an offline-first Android expense and receipt tracker. It captures or
imports expenses, keeps receipts and monthly category analytics on-device, and
syncs a durable queue to a stub remote endpoint when connectivity returns. The app
is built with Jetpack Compose, a unidirectional StateFlow architecture, Room,
WorkManager, Hilt, Retrofit, and Firebase telemetry hidden behind a testable
interface.

## Screens and features

| Screen | What it does |
|---|---|
| **Expenses** (`feature/expenses`) | Month picker, month total, day-grouped expense rows, category color, per-row sync badge, pending-sync count, add FAB, empty/loading/error states. |
| **Add / edit expense** (`feature/addexpense`) | Merchant and amount validation, integer-cent parsing, category chips, Material 3 date picker, optional note, camera receipt capture through a FileProvider URI, and CSV bulk import with per-row errors. |
| **Expense detail** (`feature/detail`) | Receipt thumbnail, amount/category/date/note, sync state, edit, and delete with confirmation. |
| **Insights** (`feature/insights`) | Selected-month total, month-over-month delta, Compose-drawn category bars, top three merchants, and pending-sync count. |
| **Background sync** (`sync`) | Unique one-shot sync after a write, 6-hour periodic sync constrained to a connected network, exponential retry, and idempotent uploads keyed on the client UUID. |
| **Telemetry** (`telemetry`) | Crashlytics non-fatals, Analytics events (`expense_created`, `receipt_captured`, `csv_imported`, `sync_completed`, `insights_viewed`), and a logcat fallback when Firebase is not configured. |

The default category set is Meals, Travel, Software, Office, Marketing, Hardware,
Fees, and Other. Unknown CSV categories fall back to Other.

## Architecture

```mermaid
flowchart TD
    UI["Compose UI<br/>screens render immutable state"] --> VM["ViewModel<br/>StateFlow + onEvent(UDF)"]
    VM --> Repo["ExpenseRepository<br/>interface"]
    Repo --> Local["Room<br/>expenses, categories, sync queue"]
    Repo --> Remote["Retrofit SpendSnapApi<br/>FakeSpendSnapApi when unset"]
    Repo --> Scheduler["SyncScheduler"]
    Scheduler --> WM["WorkManager<br/>ExpenseSyncWorker"]
    WM --> Repo
    Workers["HiltWorkerFactory"] --> WM
    VM --> Telemetry["Telemetry interface"]
    Telemetry --> Firebase["Crashlytics + Analytics"]
    Telemetry -. no google-services.json .-> Logging["LoggingTelemetry"]
```

- **`ui/`** owns the theme, navigation graph, and the four destination routes.
  The bottom bar only appears on the two top-level destinations.
- **`feature/*`** owns one screen, one immutable UI-state data class, and one
  `@HiltViewModel`. Composables read state and forward events; parsing, filtering,
  grouping, and sync decisions live below them.
- **`core/`** contains reusable `MoneyText`, `SyncBadge`, `EmptyState`,
  `ErrorBanner`, and `LoadingState` composables, plus pure `AmountParser` and
  `CsvExpenseImporter` classes.
- **`data/local`** contains Room entities, DAOs, converters, the version-1
  database, and the idempotent category seeder. Reads are `Flow` queries; writes
  mark rows `PENDING`.
- **`data/remote`** is a Retrofit interface with kotlinx.serialization DTOs. When
  `BuildConfig.API_BASE_URL` is blank, Hilt supplies `FakeSpendSnapApi`, which
  echoes a remote id, simulates latency/failure, and is idempotent by client UUID.
- **`data/repository`** reads only from Room and writes locally first. It exposes
  monthly breakdown queries and `syncPending()`.
- **`sync/`** owns the WorkManager scheduler and worker. Backoff is exponential;
  unique work uses `ExistingWorkPolicy.KEEP`.
- **`di/`** keeps Hilt boundaries at the application singleton scope: database,
  network, repository, scheduler, telemetry, and worker factory. This lets tests
  replace Room, the API, and the scheduler without touching ViewModels.
- **`telemetry/`** is an interface with a guarded Firebase implementation and a
  no-op/logcat fallback. Tests use `FakeTelemetry`.

## How to run

### Prerequisites

- JDK 17 or newer (this build was executed with OpenJDK 21.0.9; CI uses JDK 17).
- Android SDK Platform 34 and Build Tools 34.0.0.
- An emulator or device for `connectedDebugAndroidTest`.

Create `local.properties` if Android Studio has not already done so:

```properties
sdk.dir=/path/to/Android/sdk
```

### Build and install

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

The wrapper is committed and uses Gradle 8.7. No global Gradle installation is
required.

### Tests

```bash
# JVM tests (Room/WorkManager tests run under Robolectric, no emulator needed)
./gradlew :app:testDebugUnitTest

# Instrumented tests (starts the app on a connected emulator/device)
./gradlew :app:connectedDebugAndroidTest

# Formatting, static analysis, Android lint
./gradlew :app:ktlintCheck :app:detekt :app:lintDebug

# Everything a PR must pass locally
scripts/verify.sh
```

Unit test results are written to
`app/build/reports/tests/testDebugUnitTest/index.html`. Instrumented results are
written to `app/build/reports/androidTests/connected/debug/index.html`.

### Firebase (optional)

`app/google-services.json` is gitignored. Without it the app still builds and
telemetry silently degrades to logcat. To enable Firebase, download a real
`google-services.json` for `com.spendsnap` (and `com.spendsnap.debug` for debug
builds) and place it at `app/google-services.json`. See
[`docs/FIREBASE.md`](docs/FIREBASE.md) and the placeholder shape in
`app/google-services.json.example`. Never commit the real file.

### Signed bundle

Set the four signing values as environment variables or in root
`keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`):

```bash
export SPENDSNAP_KEYSTORE_FILE=/secure/spendsnap-release.jks
export SPENDSNAP_KEYSTORE_PASSWORD=...
export SPENDSNAP_KEY_ALIAS=spendsnap
export SPENDSNAP_KEY_PASSWORD=...
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`. With no signing
material, Gradle still builds, but the bundle is unsigned, as noted below.

## Measured results

Every number below comes from commands run in this repository on October 19,
2026. The Android SDK was present at
`/opt/homebrew/share/android-commandlinetools`; an API 34 emulator was booted
locally for the instrumented run.

### JVM unit tests

Command:

```bash
/usr/bin/time -p ./gradlew :app:testDebugUnitTest --rerun-tasks --no-daemon
```

- Result: **BUILD SUCCESSFUL**
- Wall clock: **38.51 s** (`real 38.51`; Gradle reported `BUILD SUCCESSFUL in 38s`)
- Test count: **41**
- Pass rate: **41/41 = 100%**, 0 failures, 0 skipped
- JUnit-reported test execution time: **3.536 s**

| Test class | Tests |
|---|---:|
| `AmountParserTest` | 13 |
| `CsvExpenseImporterTest` | 6 |
| `CsvImportThroughputTest` | 1 |
| `OfflineFirstExpenseRepositoryTest` | 6 |
| `AddExpenseViewModelTest` | 3 |
| `ExpenseListViewModelTest` | 2 |
| `InsightsCalculatorTest` | 3 |
| `ExpenseSyncWorkerTest` | 4 |
| `WorkManagerSyncSchedulerTest` | 2 |
| `CrashlyticsTelemetryTest` | 1 |
| **Total** | **41** |

### Instrumented tests

Command:

```bash
./gradlew :app:connectedDebugAndroidTest
```

- Result: **BUILD SUCCESSFUL**
- Environment: local `SpendSnapApi34` AVD, API 34, `google_apis`, arm64-v8a
- Tests: **5 passed, 0 failed, 0 skipped**
- JUnit-reported execution time: **8.211 s**
- Gradle task wall clock: **36 s**

| Class | Tests | Coverage |
|---|---:|---|
| `SpendSnapDatabaseTest` | 2 | In-memory Room CRUD, monthly query, synced state, seed categories |
| `ExpenseListScreenTest` | 1 | First-launch empty state |
| `AddExpenseScreenTest` | 2 | Add expense appears with PENDING badge; invalid amount shows validation |

These were actually executed here, not just written. The GitHub Actions
`instrumented-test` job is defined and uses the same API 34 command, but it has
not been executed by GitHub from this local environment.

### Release bundle

Command:

```bash
./gradlew :app:bundleRelease
```

- Result: **BUILD SUCCESSFUL**
- Artifact: `app/build/outputs/bundle/release/app-release.aab`
- Size: **6,666,773 bytes = 6.36 MiB (about 6.4 MB)**
- Signature: **unsigned locally** because no keystore was configured. CI signs the
  same artifact when the keystore secrets are present.
- R8 mapping: `app/build/outputs/mapping/release/mapping.txt`

Static analysis also passed:

```bash
./gradlew :app:ktlintCheck :app:detekt :app:lintDebug
```

- `ktlintCheck`: passed
- `detekt` (`config/detekt/detekt.yml`, `maxIssues: 0`): passed
- `lintDebug`: **BUILD SUCCESSFUL**

### Source size

Measured with `find` / `wc -l`:

```bash
find app/src -name '*.kt' | wc -l
# 67
find app/src -name '*.kt' -print0 | xargs -0 wc -l | tail -1
# 5416 total
```

Breakdown: 50 Kotlin files in `app/src/main`, 17 Kotlin test files across
`src/test` and `src/androidTest`; 5,416 Kotlin lines total. This count excludes
Gradle Kotlin DSL scripts and generated code.

### CSV import throughput

`CsvImportThroughputTest` generates a 1,000-data-row CSV in a JVM test, calls
`CsvExpenseImporter.parse`, and records the measured result:

```text
CSV_THROUGHPUT rows=1000 valid=995 invalid=5 elapsed_ms=36.829084
```

So: **36.83 ms** to parse 1,000 rows, **995 valid** and **5 invalid** (the invalid
rows deliberately contain `not-an-amount`). The test asserts those exact counts.

### Sync behavior

From `ExpenseSyncWorkerTest` (success, retry, partial failure, transport failure):

| Scenario | Synced | Failed | Remaining | Worker outcome |
|---|---:|---:|---:|---|
| All records accepted | 3 | 0 | 0 | success, no retry |
| All records failed | 0 | 2 | 2 | retry |
| Partial failure | 2 | 1 | 1 | retry with successful rows retained |
| Transport exception | 0 | 1 | -1 (unknown) | retry + non-fatal recorded |

`WorkManagerSyncSchedulerTest` also verifies that two immediate sync requests keep
the same unique work id, and that the periodic job is unique.

### Feedback-driven fixes

From [`docs/FEEDBACK_LOG.md`](docs/FEEDBACK_LOG.md):

1. **Locale-specific decimal separators** (Crashlytics non-fatal): the parser now
   normalizes locale digits and separators. Regression tests
   `AmountParserTest.localeSpecificDecimalSeparator_isSupported` and
   `AmountParserTest.arabicIndicDigits_areSupported` were red before the fix and
   green after it.
2. **Duplicate sync uploads** (tester feedback): unique `KEEP` work plus
   idempotent upload keyed on the client UUID, with idempotent Room `@Upsert`.
   Covered by
   `WorkManagerSyncSchedulerTest.repeatedImmediateSync_keepsOneUniqueWorkRequest`,
   `OfflineFirstExpenseRepositoryTest.concurrentSync_uploadsExactlyOnce_andLeavesOneKeyedRow`,
   and
   `OfflineFirstExpenseRepositoryTest.repeatedUpsertForKeyedUuid_updatesInsteadOfDuplicating`.

## Release engineering

- Runbook: [`docs/RELEASE.md`](docs/RELEASE.md)
- Current release identity: **versionCode 1**, **versionName 1.0.0**
  (`app/build.gradle.kts`)
- Min/target/compile SDK: **26 / 34 / 34**
- CI gates: `.github/workflows/ci.yml`
  - `lint`: ktlint, detekt, `:app:lintDebug`
  - `unit-test`: `:app:testDebugUnitTest` plus HTML report artifact
  - `instrumented-test`: API 34 emulator with `:app:connectedDebugAndroidTest`
  - `bundle`: signed `:app:bundleRelease` when keystore secrets exist, with AAB artifact
- Tagged releases: `.github/workflows/release.yml` on `v*` builds and uploads the
  signed AAB.

## Limitations and next steps

- The remote endpoint is a fake/stub. There is no authentication, real backend,
  conflict resolution, or server-side delete; deleting an expense only removes the
  local row.
- Receipt capture compiles and is wired to `ActivityResultContracts.TakePicture`,
  but the camera path was not exercised by the emulator tests. OCR and receipt
  parsing are not implemented.
- Multi-currency storage exists (`currency` on each expense), but there is no FX
  conversion; analytics sum nominal cents.
- Room is at schema version 1. The schema is exported and committed, but there is
  no second version or migration test yet.
- Instrumented coverage is deliberately narrow: list empty state, add flow,
  validation, and database behavior. Detail and Insights are covered by pure
  calculator tests and previews, not by Compose instrumented tests.
- WorkManager uniqueness and worker outcomes are tested under Robolectric; real
  periodic execution and battery behavior were not soak-tested.
- The local bundle is unsigned. A Play-ready signed bundle requires the keystore
  secrets documented in `docs/RELEASE.md`.
- Firebase events require a real `google-services.json`; without it the fallback
  writes to logcat.
- Next steps: implement a real sync contract with tombstones for deletion, add
  schema v2 + migration tests, add screenshot tests for all screens, and add
  receipt OCR with user confirmation.
