# SpendSnap release runbook

## 1. One-time signing setup

Create a 10,000-day RSA upload key. Keep the `.jks` and passwords out of git
(`.gitignore` already excludes them).

```bash
keytool -genkeypair -v \
  -keystore spendsnap-release.jks \
  -alias spendsnap \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

For local signed builds, create `keystore.properties` in the repository root:

```properties
storeFile=/absolute/path/to/spendsnap-release.jks
storePassword=...
keyAlias=spendsnap
keyPassword=...
```

CI uses environment variables instead: `SPENDSNAP_KEYSTORE_FILE`,
`SPENDSNAP_KEYSTORE_PASSWORD`, `SPENDSNAP_KEY_ALIAS`, `SPENDSNAP_KEY_PASSWORD`.
If any value is absent, `bundleRelease` still completes but produces an unsigned
bundle. That is useful for build verification, not for Play upload.

## 2. Version policy

`app/build.gradle.kts` owns release identity:

```kotlin
val appVersionCode = 1
val appVersionName = "1.0.0"
```

- Bump `appVersionCode` for every upload, including re-uploads. Play never accepts
  the same code twice.
- Semver `appVersionName`: patch for fixes, minor for a feature set, major for a
  compatibility break.
- Tag the commit that contains those exact values (`v1.0.0`) only after the CI
  bundle is green. The tag workflow does not bump versions, so the tag and bundle
  always match the committed constants.

## 3. Build and validate locally

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:ktlintCheck :app:detekt :app:lintDebug
./gradlew :app:bundleRelease
ls -lh app/build/outputs/bundle/release/app-release.aab
```

The signed output is `app/build/outputs/bundle/release/app-release.aab`. The
release build enables R8 and resource shrinking. Keep the generated
`app/build/outputs/mapping/release/mapping.txt` for deobfuscating Crashlytics
stack traces (Crashlytics normally uploads it during the Gradle plugin run).

## 4. Play Console internal testing

1. Play Console → SpendSnap → Testing → Internal testing → Create new release.
2. Upload the `.aab`. Play derives the version from `versionCode`.
3. Add release notes and resolve any pre-launch report issues.
4. Add internal testers / email list, then roll out to Internal testing.
5. Install through the opt-in URL and verify: create an expense offline, enable
   network, confirm the sync badge flips to Synced, open Insights and check the
   month totals. Firebase events should show `expense_created` and
   `sync_completed` in DebugView.

## 5. Production promotion

1. Run a closed test first. Fix blockers and upload a new `versionCode`.
2. Internal testing → Promote release → Closed testing / Production.
3. Choose a staged rollout (for example 10% → 50% → 100%) and monitor
   Crashlytics, ANRs, and the Play pre-launch report between stages.
4. Keep the previous working bundle and mapping file archived.

## 6. Rollback

Play does not let you upload a lower `versionCode` and does not "downgrade" an
installed app. The safe rollback is:

1. Halt the staged rollout immediately in Play Console.
2. Rebuild the last known-good commit with a **higher** `versionCode`, or promote
   that artifact if it is already in a track.
3. Roll out to Internal testing, verify, then promote to production.
4. If only a server/backend change is at fault, revert the server change first;
   the client bundle can remain installed.

Document incident timestamps and the exact version codes in the release notes.

## 7. CI secrets

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Base64 of the upload keystore (`base64 -i spendsnap-release.jks`) |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (normally `spendsnap`) |
| `KEY_PASSWORD` | Key password |
| `GOOGLE_SERVICES_JSON` | Optional real Firebase config for release instrumentation |

`ci.yml` gates merges on static analysis, unit tests, and emulator tests.
`release.yml` runs on `v*` tags and uploads the signed AAB artifact.
