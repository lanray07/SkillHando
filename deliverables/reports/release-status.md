# SkillHando release evidence

| Area | Status | Evidence |
| --- | --- | --- |
| Debug APK, unsigned release APK and instrumentation APK | PASS | Gradle build and release vital lint logs |
| Amazon signed production APK | PASS | Version 1.0.0, code 1. Package listing key, production billing settings, alignment and signatures checked. Amazon accepted the signed APK in the app draft. |
| Practice engine logic | PASS | Eight JVM tests cover repeat and advance recommendations, comparable attempts, tie-breaking, elapsed time, voice phrase matching and CSV escaping |
| Room on Fire HD 10 (2021), KFTRWI, Fire OS 7 / API 28 | PASS | Two device tests verify reopen persistence, attempt and evidence transaction, cascade deletion and pathway capacity |
| Offline practice journey on Fire HD 10 | PASS | In airplane mode with no active network, the Compose test saved and compared two attempts, checked a next challenge, used a typed voice command and saved a weekly goal. The rendered Skills screen was checked separately using ADB capture |
| Runtime voice | NOT TESTED | Fire HD 10 advertises IVONA and Pico TTS. No speech recognition service was advertised. Audible playback, locale coverage and microphone-denial behaviour remain unverified |
| Offline practice coach | PASS | Recommendation unit tests pass. The coach uses deterministic catalogue matching and completed attempts |
| Remote generative AI | NOT TESTED | Remote coaching is absent from this implementation |
| Translation structure | PASS | 19 interface locales have structurally validated drafts with 277 keys and all 21 challenges. Source locale is en-GB |
| Language, regional terminology, RTL and accessibility | PARTIAL | Storefront locale rendering was checked on Fire HD 10. Native review, pluralisation, RTL and accessibility checks remain |
| Store metadata | PASS | 15 editable storefront drafts exist, pending native and publisher review |
| Amazon App Tester purchase and restore | PASS | Fire HD 10 activated Premium on the first check after a fresh checkout (1.8 seconds) and passed a separate restore test after clearing the entitlement cache. Both used Amazon RVS Cloud Sandbox. |
| Amazon production pricing and verifier | CONFIGURED | £4.99 draft base price with Amazon conversion. The deployed Cloudflare verifier reached Amazon production RVS, rejected a synthetic receipt and returned HTTP 429 when rate-limited. |
| Amazon DevTest and real production purchasing | PENDING | Amazon accepted the app submission and approved the production entitlement. Verification of a valid production receipt remains pending. |
| Amazon Live App Testing | PASS | The Fire HD 10 installed the private beta from Amazon Appstore. A fresh purchase after the tester entitlement reset activated Premium in the same session without using Restore purchases. A separate restore test also verified the LAT receipt. |
| Private audio, photo, reminders, exports and deletion UI | NOT TESTED | Feature implementations require device-level verification. Optional app lock and encrypted backup are absent |
| Store screenshots | PASS | Amazon's production draft retained 88 distinct Fire tablet screenshots after reload, eight for each of the 11 media locales offered by the console. The repository also contains captures for four additional text locales and compact layouts. Native language review and physical phone compatibility remain pending. |
| Amazon submission | SUBMITTED | Amazon confirmed receipt of version 1.0.0 on Nov 14, 2026. The app showed Under Review and the production Premium SKU `com.skillhando.app.premium` showed Approved. |

The physical tablet reports 1200 × 1920 pixels at 240 dpi. The build uses app-private storage, disables Android backup and cleartext traffic, and requests microphone access on demand. These decisions have not undergone a full security review. Production purchase testing is pending.

## Next update

Version 1.0.1 (code 2) adds Korean (`ko-KR`), Simplified Chinese (`zh-CN`) and Traditional Chinese (`zh-TW`). Each locale covers 277 interface strings, 21 challenges, reminder notifications and seven command phrases, with matching store copy and eight screenshots per layout. Native-language review of the full app copy and testing with installed speech engines remain pending. In the next Amazon submission, list Korean and Chinese in Language Support. Amazon's storefront localization settings do not offer these variants for listing copy or images. [Amazon international distribution](https://developer.amazon.com/docs/app-submission/distribute-your-app-internationally.html)

## Commands

```powershell
.\gradlew.bat --offline :app:assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
python tools/validate_project.py
python tools/localize.py --validate
.\tools\fire-qa.ps1 -Adb <path-to-adb.exe> -Airplane
```
