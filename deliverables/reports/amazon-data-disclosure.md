# Amazon privacy disclosure worksheet

Answer **Yes** to whether the app collects user data or transfers it to third parties. Amazon requires a public privacy policy URL and a completed questionnaire for this answer. The policy draft is in `deliverables/store-assets/privacy-policy.md`.

| Amazon data type | Collected | Third-party transfer | Purpose |
| --- | --- | --- | --- |
| User IDs | Yes | Yes, Amazon purchase verification | App functionality |
| Purchase history | Yes, Amazon receipt identifiers and entitlement status | Yes, Amazon purchase verification | App functionality |
| Device or other IDs | Yes, connecting IP for verifier throttling | Cloudflare acts as the verifier's service provider | Security and fraud prevention |
| Voice or sound recordings | Yes, when the user invokes a speech engine that processes voice online | Yes for provider-dependent online recognition | App functionality |

Practice notes, photos, locally recorded evidence and other private practice records stay on-device. The user can explicitly export files or share them through Android. The app receives no payment instrument details and includes no ads or developer analytics. There is no SkillHando account. Users can delete practice records and attachments from **More → Privacy settings**. Amazon administers the separate Amazon account and purchase history.

Before publishing the policy, confirm the publisher identity, support email handling and retention, and which speech providers are supported. Review the answers against the live Amazon questionnaire, especially its service-provider and user-initiated-transfer exceptions. [Amazon Appstore privacy labels](https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html)
