# Release checklist

## Build and distribution

- Use `assembleDebug` to install locally and `assembleRelease` to package a non-debuggable APK.
- Select the actual Fire tablet device matrix in Amazon Developer Console. Confirm the package name and version with the publisher’s app record.
- Choose the signing and distribution workflow, check the release certificate when applicable, and retain the signing materials securely. [Amazon Appstore SDK and signing guidance](https://developer.amazon.com/docs/in-app-purchasing/iap-get-started.html)
- Complete privacy and content disclosures, icon and screenshot uploads, support contact, metadata and locale selections. [Amazon Appstore details](https://developer.amazon.com/docs/app-submission/appstore-details.html)

## QA evidence to collect

- Run the two-attempt practice journey with the device offline. Verify that text, evidence and ratings survive a process restart.
- Validate microphone consent, live audio capture and playback, photo import, CSV and PDF content, local deletion, quiet hours and notification privacy.
- Test English and every approved locale on representative Fire tablets. Include text expansion, screen readers, manual locale switching, dark mode and Arabic RTL.
- Verify positive and negative Amazon receipt responses, cancellation, revocation, restore, account changes and offline behaviour with App Tester and Live App Testing. [Amazon IAP testing](https://developer.amazon.com/docs/in-app-purchasing/iap-testing-overview.html)
- Confirm publisher support information, privacy policy, trademark review and store acceptance criteria.

## Capability gaps

- Offline coaching recommends existing challenges. Optional hosted AI, schema-validated generative plans and a remote usage policy still need implementation.
- App lock and encrypted backup and restore need implementation. Standard Android app sandboxing protects local records.
- A single configurable daily practice reminder is implemented. Separate milestone and custom reminder schedules need implementation.
- Human review is pending for translation and listing drafts. Review regional terminology and screen captures before approving a storefront language.
