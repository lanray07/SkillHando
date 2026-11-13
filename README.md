# SkillHando

SkillHando is a native Android practical-skills app. Practice starts with an observable baseline and one change to test. Each attempt records what changed, a self-assessment and optional evidence. Progress compares completed attempts of the same challenge and recommends when to repeat the exercise.

## Build and test

Use JDK 17, Android SDK platform 35 and Android build tools 35.0.0. Set `ANDROID_HOME` or add `sdk.dir` to a local `local.properties` file. Minimum Android API is 26.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
python tools/localize.py --validate
```

Only the debug APK is automatically signed for local installation. Gradle creates an unsigned release APK. For direct installation, align and sign an APK with the publisher’s selected release key. Keep keystores and passwords outside the project. Confirm Amazon’s current signing and submission workflow before uploading.

## Architecture

Compose screens observe the Room store through a lifecycle-aware view model. The practice engine compares complete attempts with identical skill and challenge IDs. The timer uses Android elapsed realtime and checks the boot counter when resuming. Evidence files live in app-private storage and become visible in the portfolio only when the attempt and attachment records commit together. WorkManager delivers optional approximate reminders. The catalogue is bundled with the app.

## Localisation

The language control loads locale-specific interface and catalogue data, with regional English fallback. `tools/localize.py` generates and structurally checks machine translation drafts. Use a local CTranslate2-compatible M2M100 model and tokenizer or supply reviewed JSON dictionaries with the same schema. See the coverage report for generated files. Runtime voice availability varies with the installed Android recognizer and TTS voice. Command mappings cover the 14 base languages represented by the 19 interface locales. Physical device support and regional dictation need verification.

## Amazon purchasing

Pass `-PskillhandoAmazonSku=<entitled-sku>`, `-PskillhandoVerifierUrl=https://your-host/verify` and, when required, `-PskillhandoAmazonAuthFile=<path-to-AppstoreAuthenticationKey.pem>`. The app receives the formatted price from Amazon, handles restore, and grants Premium after successful server verification. See `.env.example` for server settings. Run `python billing/server.py` behind a rate-limited HTTPS reverse proxy and use the matching Amazon receipt environment. Keep the RVS shared secret server-side. Verify purchase, cancellation, refund and restore on App Tester or Live App Testing before release.

## Release scope

Amazon distribution targets Fire tablets. Android phones can be tested by sideload. The one-time purchase model has no recurring inference costs. The practice coach runs locally and uses history and catalogue matching. Remote generative coaching and private audio analysis are outside the current implementation. See the release evidence in `deliverables`.
