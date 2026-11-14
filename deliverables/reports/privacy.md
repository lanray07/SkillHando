# SkillHando privacy draft

SkillHando stores practice notes, ratings, photos, audio, learning goals and preferences in app-private storage on the device. Android app backup is disabled. The app does not require an account, show ads or record audio in the background. The local practice coach uses bundled content and completed practice records on the device.

The selected speech engine may process microphone input online. The app requests microphone access when you choose to speak a command or make an audio recording. SkillHando does not save recordings of voice commands. Audio evidence is stored locally. The data deletion control in More removes the practice database records and private attachments. Exports are saved or shared only at your direction. You control those copies.

When Amazon purchasing is enabled, Amazon handles checkout. The verifier runs on Cloudflare Workers and forwards the receipt identifier and Amazon user identifier to Amazon to check the entitlement. Cloudflare provides the request IP address for rate limiting. The verifier has no receipt database, and Worker observability is disabled. It does not receive practice records or attachments. Review provider retention and support contact details before publishing this disclosure.

## Release checklist

- Review platform backup exclusion, component exposure, file handling, data deletion and receipt-verifier deployment.
- Confirm store data declarations and the support contact with the publisher.
- Complete verification of recordings, private export consent and deletion on supported devices.
- Encrypted backups and an optional app lock require implementation.
