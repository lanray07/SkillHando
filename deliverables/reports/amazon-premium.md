# Amazon Premium verification

## App Tester on Fire HD 10

- Device: Fire HD 10 (2021), Fire OS 7 / API 28. Amazon App Tester version 3.0.200459.0_3080810.
- Checkout displayed the test-only entitlement and US$4.99. Amazon App Tester showed its order-complete screen after **Get Item**.
- A follow-up run cleared the isolated app’s saved entitlement. It recovered the sandbox receipt through the Amazon SDK and processed an explicit **Restore purchases** request. The local server received two valid HTTP 200 responses from Amazon RVS Cloud Sandbox. The app showed **Premium purchase verified** and **Premium is active**.
- The device’s existing App Tester catalogue was restored and USB port forwarding was removed. App Tester checkout is simulated and did not incur a charge.
- The isolated debug package is `com.skillhando.app.billingtest`. USB reverse connects it to the loopback verifier. Release builds require HTTPS and exclude the debug network policy.

## Developer Console

- The SkillHando app draft was saved under Education with app SKU `com.skillhando.app`.
- The one-time SkillHando Premium entitlement was saved with SKU `com.skillhando.app.premium`. Store pricing and production verification still need configuration. [Premium entitlement draft](https://developer.amazon.com/apps-and-games/console/app/amzn1.devportal.mobileapp.075fedf359ae487d90214effe438def2/iap/entitlement/amzn1.dex.title.2a5f051edb49413e96e7d6e905c45f91/detail)
- DevTest console registration and production purchase verification remain pending. Amazon requires a signing certificate, registered tester access and a live in-app item for DevTest. The listing and entitlement were left as drafts. [DevTest prerequisites](https://developer.amazon.com/docs/app-testing/get-started-appstore-devtest-iap.html#prerequisites)

[SkillHando draft](https://developer.amazon.com/apps-and-games/console/app/amzn1.devportal.mobileapp.075fedf359ae487d90214effe438def2/release/amzn1.devportal.apprelease.696e69c4b6544cd48f2e7d64b846e900/submission/app-files)
