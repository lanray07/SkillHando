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

### App Tester on a Fire tablet

Run an isolated build with `.\gradlew.bat -PskillhandoBillingSandbox=true :app:assembleDebug`. This produces the `com.skillhando.app.billingtest` package. Start `billing/server.py` on loopback port 18744 using `SKILLHANDO_RVS_MODE=sandbox`, `SKILLHANDO_RVS_PRODUCT=com.skillhando.testing.premium`, and any nonempty sandbox secret. The PowerShell helper `tools/billing_sandbox.ps1` temporarily merges the test SKU into App Tester, switches its mode, and forwards the verifier port over USB. Run `tools/check_sandbox_purchase.py` in its check block with the appropriate stage. Use **open** for fresh test checkout, **verify** to check the post-checkout screen, and **restore** after closing the isolated app and clearing only its cached entitlement. [Amazon RVS Cloud Sandbox](https://developer.amazon.com/docs/in-app-purchasing/rvs-cloud-sandbox.html)

### Store screenshots

Build the isolated screenshot APK and its test APK with `.\gradlew.bat -PskillhandoScreenshotBuild=true :app:assembleDebug :app:assembleDebugAndroidTest`. Install both and run `tools/capture_store_screens.ps1` with the ADB path, Fire tablet serial and Japanese font path. Use `python tools/validate_store_screens.py` to regenerate the combined manifest. The phone-layout option uses temporary display overrides on the Fire tablet.

## Release scope

Amazon distribution targets Fire tablets. Android phones can be tested by sideload. The one-time purchase model has no recurring inference costs. The practice coach runs locally and uses history and catalogue matching. Remote generative coaching and private audio analysis are outside the current implementation. See the release evidence in `deliverables`.
