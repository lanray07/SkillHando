# SkillHando release evidence

| Area | Status | Evidence |
| --- | --- | --- |
| Debug APK, unsigned release APK and instrumentation APK | PASS | Gradle build and release vital lint logs |
| Practice engine logic | PASS | Eight JVM tests cover repeat and advance recommendations, comparable attempts, tie-breaking, elapsed time, voice phrase matching and CSV escaping |
| Room on Fire HD 10 (2021), KFTRWI, Fire OS 7 / API 28 | PASS | Two device tests verify reopen persistence, attempt and evidence transaction, cascade deletion and pathway capacity |
| Offline practice journey on Fire HD 10 | PASS | In airplane mode with no active network, the Compose test saved and compared two attempts, checked a next challenge, used a typed voice command and saved a weekly goal. The rendered Skills screen was checked separately using ADB capture |
| Runtime voice | NOT TESTED | Fire HD 10 advertises IVONA and Pico TTS. No speech recognition service was advertised. Audible playback, locale coverage and microphone-denial behaviour remain unverified |
| Offline practice coach | PASS | Recommendation unit tests pass. The coach uses deterministic catalogue matching and completed attempts |
| Remote generative AI | NOT TESTED | Remote coaching is absent from this implementation |
| Translation structure | PASS | 19 interface locales have structurally validated drafts with 277 keys and all 21 challenges. Source locale is en-GB |
| Language, regional terminology, RTL and accessibility | NOT TESTED | Native review, pluralisation and rendered locale checks remain |
| Store metadata | PASS | 15 editable storefront drafts exist, pending native and publisher review |
| Amazon purchasing | BLOCKED | Amazon SDK and RVS verifier source are present. Three local verifier tests passed with canned Amazon responses. Actual entitlement SKU, authentication material, deployed HTTPS verifier and a configured test environment are required |
| Private audio, photo, reminders, exports and deletion UI | NOT TESTED | Feature implementations require device-level verification. Optional app lock and encrypted backup are absent |
| Store screenshots | PASS | Eight en-GB landscape drafts from real Fire HD 10 screens, checked visually and exported at 1920 × 1080. Other storefront locales and phone variants remain outstanding |
| Submission readiness | BLOCKED | Complete device testing, monetisation, translation review, asset validation and publisher submission steps before authorising a release |

The physical tablet reports 1200 × 1920 pixels at 240 dpi. The build uses app-private storage, disables Android backup and cleartext traffic, and requests microphone access on demand. These decisions have not undergone a full security review. No Amazon Console submission or production purchase occurred.

## Commands

```powershell
.\gradlew.bat --offline :app:assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
python tools/validate_project.py
python tools/localize.py --validate
.\tools\fire-qa.ps1 -Adb <path-to-adb.exe> -Airplane
```
