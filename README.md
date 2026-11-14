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

The production SKU is `com.skillhando.app.premium`. Build the production-configured unsigned APK with:

```powershell
.\gradlew.bat -PskillhandoAmazonSku=com.skillhando.app.premium -PskillhandoVerifierUrl=https://skillhando-receipt-verifier.lanraybanks.workers.dev/verify :app:assembleRelease
```

For Amazon packaging, supply the listing’s Appstore public key with `-PskillhandoAmazonAuthFile=<path-to-AppstoreAuthenticationKey.pem>`. The app receives the formatted price from Amazon and grants Premium after server verification. Cloudflare runs the production verifier in `billing/worker.mjs`. Deploy changes with `npx wrangler@4.156.0 deploy --config billing/wrangler.jsonc` and keep the account’s Amazon RVS shared key in the Worker’s `SKILLHANDO_RVS_SECRET` secret binding. The Worker checks the production SKU, entitlement type, receipt ID, cancellation and Amazon test flags. Use the isolated setup below for App Tester and a separate verifier environment for any future Live App Testing receipt checks. [Amazon production RVS](https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html)

The signed production upload is `deliverables/android/release/SkillHando-amazon.apk`. The dedicated upload keystore is retained outside Git under `%USERPROFILE%\.codex\signing\skillhando-production`, with a password protected by Windows DPAPI. For a new upload, build with the public key, run Android SDK `zipalign` before signing, and run `apksigner verify --print-certs` on the result. Amazon applies its own distribution signature. [Amazon Appstore signing guidance](https://developer.amazon.com/docs/in-app-purchasing/iap-get-started.html)

The private Live App Testing build uses SKU `com.skillhando.app.premium.lat` and `https://skillhando-receipt-verifier-lat.lanraybanks.workers.dev/verify`. Pass these values to the same Gradle options and use the same listing public key. Deploy the separate verifier with `npx wrangler@4.156.0 deploy --config billing/wrangler.jsonc --env lat`. Its secret binding is configured separately, and it checks Amazon-issued test receipts for the LAT SKU. [Live App Testing for IAP](https://developer.amazon.com/docs/in-app-purchasing/iap-lat.html)

### App Tester on a Fire tablet

Run an isolated build with `.\gradlew.bat -PskillhandoBillingSandbox=true :app:assembleDebug`. This produces the `com.skillhando.app.billingtest` package. Start `billing/server.py` on loopback port 18744 using `SKILLHANDO_RVS_MODE=sandbox`, `SKILLHANDO_RVS_PRODUCT=com.skillhando.testing.premium`, and any nonempty sandbox secret. The PowerShell helper `tools/billing_sandbox.ps1` temporarily merges the test SKU into App Tester, switches its mode, and forwards the verifier port over USB. Before a fresh run, delete only the SkillHando test transaction from App Tester and clear the isolated app’s cached entitlement. Run `tools/check_sandbox_purchase.py --stage checkout` inside the helper’s check block, with the ADB path and tablet serial, to check activation in the same app session. Use **restore** to test the explicit restore action after clearing the isolated app’s cached entitlement and relaunching. [Amazon RVS Cloud Sandbox](https://developer.amazon.com/docs/in-app-purchasing/rvs-cloud-sandbox.html)

### Store screenshots

Build the isolated screenshot APK and its test APK with `.\gradlew.bat -PskillhandoScreenshotBuild=true :app:assembleDebug :app:assembleDebugAndroidTest`. Install both and run `tools/capture_store_screens.ps1` with the ADB path, Fire tablet serial and Japanese font path. Use `python tools/validate_store_screens.py` to regenerate the combined manifest. The phone-layout option uses temporary display overrides on the Fire tablet.

## Release scope

Amazon distribution targets Fire tablets. Android phones can be tested by sideload. The one-time purchase model has no recurring inference costs. The practice coach runs locally and uses history and catalogue matching. Remote generative coaching and private audio analysis are outside the current implementation. See the release evidence in `deliverables`.
