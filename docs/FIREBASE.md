# Firebase setup (Crashlytics + Analytics)

SpendSnap compiles and runs without Firebase credentials. In that mode
`CrashlyticsTelemetry` detects that no `FirebaseApp` exists and delegates to
`LoggingTelemetry`, so events go to logcat instead of crashing the app.

To enable the real SDKs locally:

1. Create a Firebase Android app for `com.spendsnap` and a second debug app for
   `com.spendsnap.debug`.
2. Download the real `google-services.json` and place it at `app/google-services.json`.
   This path is gitignored. Never commit it.
3. Copy `app/google-services.json.example` only as a shape reference; its values are
   placeholders and will not connect to a project.
4. Rebuild. The app Gradle script applies the Google Services and Crashlytics plugins
   only when `app/google-services.json` exists.
5. In CI, write the file from the `GOOGLE_SERVICES_JSON` secret before `bundleRelease`.

Useful checks:

```bash
test -f app/google-services.json && echo configured
./gradlew :app:assembleDebug
```
