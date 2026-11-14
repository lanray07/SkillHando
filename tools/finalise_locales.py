"""Sync locale declarations and generated notification strings with the available drafts."""
import json
from pathlib import Path
from xml.sax.saxutils import escape
from localize import ROOT, ASSETS, LOCALES, read

resources = ROOT / "app/src/main/res"
strings = {
    "en": ("Time for a little practice", "Open SkillHando to pick up where you left off.", "Practice reminders", "Snooze for an hour"),
    "es": ("Hora de practicar", "Abre SkillHando para continuar.", "Recordatorios de práctica", "Posponer una hora"),
    "fr": ("Et si vous repreniez votre pratique ?", "Ouvrez SkillHando pour reprendre votre exercice.", "Rappels de pratique", "Reporter d’une heure"),
    "de": ("Zeit für eine kurze Übung", "Öffne SkillHando und übe weiter.", "Übungserinnerungen", "Eine Stunde später erinnern"),
    "it": ("È ora di fare pratica", "Apri SkillHando per riprendere da dove eri.", "Promemoria di pratica", "Rimanda di un’ora"),
    "pt": ("Hora de praticar", "Abra o SkillHando para retomar a prática.", "Lembretes de prática", "Adiar uma hora"),
    "nl": ("Tijd om even te oefenen", "Open SkillHando om verder te gaan.", "Oefenherinneringen", "Een uur uitstellen"),
    "pl": ("Czas na krótką praktykę", "Otwórz SkillHando i wróć do ćwiczeń.", "Przypomnienia o ćwiczeniach", "Przypomnij za godzinę"),
    "sv": ("Dags att öva en stund", "Öppna SkillHando för att fortsätta.", "Övningspåminnelser", "Påminn om en timme"),
    "ja": ("少し練習しませんか", "SkillHandoを開いて、続きから練習しましょう。", "練習リマインダー", "1時間後に通知"),
    "hi": ("थोड़ा अभ्यास करने का समय", "अभ्यास जारी रखने के लिए SkillHando खोलें।", "अभ्यास अनुस्मारक", "एक घंटे बाद याद दिलाएँ"),
    "ar": ("حان وقت التدريب", "افتح تطبيق SkillHando لمتابعة التدريب.", "تذكيرات التدريب", "التأجيل لمدة ساعة"),
    "tr": ("Biraz pratik yapma zamanı", "Kaldığın yerden devam etmek için SkillHando’yu aç.", "Pratik hatırlatıcıları", "Bir saat ertele"),
    "id": ("Waktunya berlatih sebentar", "Buka SkillHando untuk melanjutkan latihan.", "Pengingat latihan", "Tunda satu jam"),
    "ko": ("잠깐 연습할 시간이에요", "SkillHando를 열어 하던 연습을 이어 가세요.", "연습 알림", "한 시간 후 다시 알림"),
    "zh-CN": ("该练习一会儿了", "打开 SkillHando，接着上次的进度练习。", "练习提醒", "一小时后提醒"),
    "zh-TW": ("該練習一下了", "開啟 SkillHando，接著上次的進度練習。", "練習提醒", "一小時後提醒"),
}

overrides = read(ROOT / "tools/locale-overrides.json")
regional_resources = {"zh-CN": ("b+zh+Hans", "zh-rCN"), "zh-TW": ("b+zh+Hant", "zh-rTW")}
available = [tag for tag in LOCALES if tag == "en-GB" or (ASSETS / "i18n" / f"{tag}.json").is_file()]
for tag in available:
    target = ASSETS / "i18n" / f"{tag}.json"
    if target.exists():
        draft = read(target)
        draft.update(overrides.get(tag, {}))
        target.write_text(json.dumps(draft, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lang = tag.split("-")[0]
    if lang == "en":
        continue
    keys = ("reminder_title", "reminder_body", "reminder_channel", "snooze")
    labels = strings[tag] if tag in strings else strings[lang]
    entries = "\n".join(f'    <string name="{key}">{escape(value)}</string>' for key, value in zip(keys, labels))
    for qualifier in regional_resources.get(tag, (lang,)):
        dest = resources / ("values-" + qualifier)
        dest.mkdir(exist_ok=True)
        (dest / "notifications.xml").write_text(f"<resources>\n{entries}\n</resources>\n", encoding="utf-8")

declared = []
for tag in available:
    declared.append(tag)
    if tag == "en-AU":
        declared.append("en-IN")
    elif tag == "es-419":
        declared.append("es-MX")
entries = "\n".join(f'    <locale android:name="{tag}" />' for tag in declared)
(resources / "xml/locales.xml").write_text(
    '<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n' + entries + "\n</locale-config>\n", encoding="utf-8")
print(f"Generated locale declaration for {len(available)} locales and {len(set(tag.split('-')[0] for tag in available) - {'en'})} notification resource sets.")
