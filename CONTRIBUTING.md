# Contributing to SpendSnap

## Branching

- `main` is the release line and must stay green.
- Use `feat/<short-topic>`, `fix/<short-topic>`, `test/<short-topic>`, or
  `docs/<short-topic>` branches.
- Keep one logical change per pull request. Rebase on `main` before opening it.

## Commit convention

Use Conventional Commits:

```text
feat: add receipt thumbnail placeholder
fix: keep locale decimal separator when parsing amounts
test: cover duplicate sync retry
docs: document Play internal testing
chore: bump Gradle wrapper
```

The subject is imperative, lowercase after the prefix, and under 72 characters.
Reference the affected screen or package in the body when useful.

## Local checks before opening a PR

```bash
scripts/verify.sh
./gradlew :app:connectedDebugAndroidTest   # requires an emulator/device
```

`scripts/verify.sh` runs ktlint, detekt, JVM tests, and `assembleDebug`.

## How CI gates merges

`.github/workflows/ci.yml` runs on pushes and pull requests:

1. **lint** — `:app:ktlintCheck`, `:app:detekt`, `:app:lintDebug`
2. **unit-test** — `:app:testDebugUnitTest` and an HTML report artifact
3. **instrumented-test** — `:app:connectedDebugAndroidTest` on an API 34 emulator
4. **bundle** — signed `:app:bundleRelease` when keystore secrets exist, with AAB artifact

All jobs must pass before squash-merging. Do not bypass a red emulator job. If it
is flaky, document the failure and rerun; do not skip tests to get a merge.

## Privacy and secrets

- Never commit `google-services.json`, keystores, `keystore.properties`, or API keys.
- Use `app/google-services.json.example` as a shape reference only.
- Treat analytics properties as non-sensitive: no merchant names, notes, receipt
  URIs, or email addresses in event parameters.

## Accessibility and UI checklist

- Add a meaningful content description to every icon that communicates action.
- Provide `testTag`s for nodes asserted by Compose tests.
- Verify the screen has loading, empty, and error states where applicable.
- Check light and dark previews before requesting review.
