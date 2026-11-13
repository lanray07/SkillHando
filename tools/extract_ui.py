"""Extract literal interface copy. Run only when introducing new plain-text strings."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/skillhando/app"
OUT = ROOT / "app/src/main/assets/ui-en.json"
FILES = ("Dashboard", "GuidedPractice", "ProgressPage", "SettingsPage", "PremiumPage", "LearningModel", "Voice", "Billing", "Reports")
TOKEN = re.compile(r'"(?:\\.|[^"\\$\n]|\$(?!\{)|\$\{(?:[^{}"]|"(?:\\.|[^"\\])*")*\})*"')
SINGLE = set("Today Skills Practice Progress More Beginner Intermediate Advanced Paused Complete Incomplete Reflection Notes Discard Remove Rename Keep Delete Cancel Command Date Skill Challenge Seconds Status Self-ratings Attachments Baseline Experiment Observation Privacy".split())


def main():
    values = json.loads(OUT.read_text(encoding="utf-8")) if OUT.exists() else {}
    for name in FILES:
        path = SRC / (name + ".kt")
        def replace(match):
            raw = match.group()
            if match.string[max(0, match.start()-3):match.start()] == "tr(":
                return raw
            text = raw[1:-1]
            if "$" in text or "\\" in text:
                return raw
            if name == "Voice" and "to VoiceIntent" in match.string[match.end():match.end()+22]:
                return raw
            if text not in SINGLE and not (text[0:1].isupper() and (" " in text or text.endswith("…"))):
                return raw
            key = re.sub("[^a-z0-9]+", "_", text.lower()).strip("_")[:80]
            if key in values and values[key] != text:
                raise ValueError(f"Duplicate key: {key}")
            values[key] = text
            return 'tr("' + key + '")'
        path.write_text(TOKEN.sub(replace, path.read_text(encoding="utf-8")), encoding="utf-8")
    OUT.write_text(json.dumps(values, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
