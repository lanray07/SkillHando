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
| Amazon DevTest and real production purchasing | PENDING | The listing and entitlement remain drafts. DevTest requires a live in-app item and a registered signing certificate and tester. Verification of a valid SkillHando production receipt remains pending. |
| Amazon Live App Testing | IN PROGRESS | Amazon activated the private Fire OS test and approved the separate TEST entitlement. The tester invitation is pending, and the Fire HD 10 still has the sideloaded build. Amazon installation and receipt verification await tester access. |
| Private audio, photo, reminders, exports and deletion UI | NOT TESTED | Feature implementations require device-level verification. Optional app lock and encrypted backup are absent |
| Store screenshots | PASS | Eight screenshots for each of 15 storefront locales on physical Fire HD 10 and eight per locale at a compact display size on the same tablet. Native language review and physical phone compatibility remain pending. |
| Submission readiness | BLOCKED | Complete outstanding device testing, production monetisation, native translation review and publisher checks before release |

The physical tablet reports 1200 × 1920 pixels at 240 dpi. The build uses app-private storage, disables Android backup and cleartext traffic, and requests microphone access on demand. These decisions have not undergone a full security review. No Amazon Console submission or production purchase occurred.

## Commands

```powershell
.\gradlew.bat --offline :app:assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
python tools/validate_project.py
python tools/localize.py --validate
.\tools\fire-qa.ps1 -Adb <path-to-adb.exe> -Airplane
```
