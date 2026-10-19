# SpendSnap Feedback Log

This log records the two production-shaped issues fixed during the background-sync
milestone. Both fixes have regression tests that were run red before the change and
green afterwards.

| Issue | Source | Root cause | Fix commit | Verified by test |
|---|---|---|---|---|
| Amounts using a non-ASCII locale decimal separator (for example `12٫50` in `ar-EG`) were rejected. The form treated the input as invalid; an earlier amount-validation path surfaced `NumberFormatException` when the string reached `BigDecimal`. | Crashlytics non-fatal (`NumberFormatException` on the amount path) plus a tester report from an Arabic-locale device. | `AmountParser` only tokenized `.` and `,` and passed non-ASCII digits straight into `BigDecimal`. `DecimalFormatSymbols` supplies the real decimal/grouping characters and digits per locale. | Step 4 — `feat: WorkManager sync, insights screen and Crashlytics-driven fixes` | `AmountParserTest.localeSpecificDecimalSeparator_isSupported` and `AmountParserTest.arabicIndicDigits_areSupported` (both failed before the fix and pass now). |
| Duplicate expenses appeared after a sync retry or when a write landed while a sync was already running. | Tester feedback reproduced on a flaky network, confirmed by duplicate rows in the fake backend. | Work was enqueued without a stable unique name/policy, and the upload path assumed the server would create a new row on every retry. | Step 4 — `feat: WorkManager sync, insights screen and Crashlytics-driven fixes` | `WorkManagerSyncSchedulerTest.repeatedImmediateSync_keepsOneUniqueWorkRequest` (failed with `ExistingWorkPolicy.REPLACE`, passes with `KEEP`), `OfflineFirstExpenseRepositoryTest.concurrentSync_uploadsExactlyOnce_andLeavesOneKeyedRow`, and `OfflineFirstExpenseRepositoryTest.repeatedUpsertForKeyedUuid_updatesInsteadOfDuplicating`. |

## Fix 1 detail

`AmountParser` now normalizes the locale's digits to ASCII and maps the locale's
decimal and grouping separators before tokenizing. This keeps integer-cent math
in `BigDecimal` and handles data such as:

- `12٫50` in `ar-EG` → 1250 cents
- `١٢٫٥٠` in `ar-EG` → 1250 cents
- `1.234,56` in `de-DE` → 123456 cents

Failing run observed: 2 tests failed with `AmountParserTest` assertion errors.
Passing run observed after the fix: the whole `AmountParserTest` class passed.

## Fix 2 detail

Uploads are keyed by the expense UUID (`Idempotency-Key`), and the fake backend
returns the same remote id for a repeated client UUID. Room uses `@Upsert`, so
re-saving the same UUID updates one row. `WorkManagerSyncScheduler` uses one
unique name and `ExistingWorkPolicy.KEEP`, so a second write cannot replace or
duplicate in-flight one-shot work.

Failing run observed with `ExistingWorkPolicy.REPLACE`: the unit test found a
different work id after the second request. Restoring `KEEP` made the test pass.
