# Amazon Premium verification

## App Tester on Fire HD 10

- Device: Fire HD 10 (2021), Fire OS 7 / API 28. Amazon App Tester version 3.0.200459.0_3080810.
- Checkout displayed the test-only entitlement and US$4.99. Amazon App Tester showed its order-complete screen after **Get Item**.
- On 14 November 2026, a fresh order started with Premium inactive. The app displayed **Premium is active** on the first UI check, 1.8 seconds after closing the checkout. There was no relaunch or restore action. The sandbox verifier received one valid HTTP 200 response.
- A follow-up run cleared the isolated app’s saved entitlement. It recovered the sandbox receipt through the Amazon SDK and processed an explicit **Restore purchases** request. The local server received two valid HTTP 200 responses from Amazon RVS Cloud Sandbox. The app showed **Premium purchase verified** and **Premium is active**.
- The device’s existing App Tester catalogue was restored and USB port forwarding was removed. App Tester checkout is simulated and did not incur a charge.
- The isolated debug package is `com.skillhando.app.billingtest`. USB reverse connects it to the loopback verifier. Release builds require HTTPS and exclude the debug network policy.

## Developer Console

- The SkillHando app draft was saved under Education with app SKU `com.skillhando.app`.
- The one-time Premium entitlement draft (`com.skillhando.app.premium`) uses a £4.99 base list price with Amazon calculating prices for the other marketplaces. The saved price persisted after reloading. [Premium entitlement draft](https://developer.amazon.com/apps-and-games/console/app/amzn1.devportal.mobileapp.075fedf359ae487d90214effe438def2/iap/entitlement/amzn1.dex.title.2a5f051edb49413e96e7d6e905c45f91/detail)
- DevTest console registration and production purchase verification remain pending. Amazon requires a signing certificate, registered tester access and a live in-app item for DevTest. The listing and entitlement were left as drafts. [DevTest prerequisites](https://developer.amazon.com/docs/app-testing/get-started-appstore-devtest-iap.html#prerequisites)

[SkillHando draft](https://developer.amazon.com/apps-and-games/console/app/amzn1.devportal.mobileapp.075fedf359ae487d90214effe438def2/release/amzn1.devportal.apprelease.696e69c4b6544cd48f2e7d64b846e900/submission/app-files)

## Production receipt verification

- The dedicated Cloudflare Worker calls Amazon’s production RVS endpoint for the production SKU. The Amazon developer account’s shared key was installed in Cloudflare secret storage. The app and repository contain only the public HTTPS endpoint and product SKU.
- The deployed Worker returned HTTP 204 for readiness, HTTP 400 for a malformed request, and HTTP 200 with `valid: false` for a synthetic receipt checked with Amazon production RVS. A live rate-limit probe observed HTTP 429. A local test exercised successful verification with a fixture and rejection of canceled, mismatched and test receipts.
- The signed APK embeds SkillHando’s Amazon Appstore public key, the production SKU and HTTPS endpoint. APK alignment and signature schemes v2 and v3 passed. Amazon accepted the replacement APK in the production draft as version code 1. The production entitlement remained Ready to Submit.

## Live App Testing

- The private test draft contains a separately signed APK with the test SKU and verifier. The deployed test verifier reached Amazon production RVS and rejected a synthetic receipt. Fixture tests cover valid test receipts and rejection of production receipts by the test verifier.
- On 14 November 2026, Amazon approved `com.skillhando.app.premium.lat` for testing. The in-app items dashboard showed the TEST badge and Approved status for this SKU, and Ready to Submit with no submission date for the production SKU. [Amazon in-app items](https://developer.amazon.com/apps-and-games/console/app/amzn1.devportal.mobileapp.075fedf359ae487d90214effe438def2/iap/dashboard)
- Amazon requires export compliance certification and the tester rights, consent and age attestation before the private test can run. A real SkillHando receipt remains unverified. [Private SkillHando test draft](https://developer.amazon.com/apps-and-games/console/app/amzn1.devportal.mobileapp.075fedf359ae487d90214effe438def2/beta-release/amzn1.devportal.apprelease.8fbb1a5de74047259ebaeb9d5aeff74a/submit)
