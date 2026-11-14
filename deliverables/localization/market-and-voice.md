# Localisation and voice coverage

- Interface locale targets are en-GB, en-US, en-AU, es-ES, es-419, fr-FR, fr-CA, de-DE, it-IT, pt-BR, pt-PT, nl-NL, pl-PL, sv-SE, ja-JP, hi-IN, ar, tr-TR and id-ID. The coverage JSON describes the source and generated drafts.
- Amazon’s documented storefront choices are English (US, UK, Australia, India), German, Spanish (Spain, Mexico), French, Italian, Japanese, Dutch, Polish, Portuguese (Brazil, Portugal) and Swedish. The storefront may use US English as a fallback. [Amazon storefront localisations](https://developer.amazon.com/docs/app-submission/distribute-your-app-internationally.html#providing-localized-descriptions)
- Amazon English (India) uses the en-GB source text with en-IN regional formatting. Amazon Spanish (Mexico) uses es-419 content with es-MX regional formatting. Both were rendered using their own app locale setting.
- Fire tablet distribution is configured separately from interface language. Confirm final market selections in Developer Console. [Amazon marketplace and territory routing](https://developer.amazon.com/docs/app-submission/distribute-your-app-internationally.html)
- Command dictionaries use one phrase per intent per target base language plus a US English alias for “what should I practice next”. Regional variants need recognition and phrasing validation.
- Speech recognition depends on the device recognising the selected language. Installed services do not establish language coverage or offline processing.
- TTS requires an installed engine and language data. The Fire HD 10 KFTRWI advertises IVONA and Pico TTS services, with no recognition service advertised on the device.
- Translation drafts use local machine inference. Native linguistic review, locale-by-locale on-device verification, plurals and RTL layout QA remain release criteria.
