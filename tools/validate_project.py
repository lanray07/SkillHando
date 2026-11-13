from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
java = root / "app/src/main/java/com/skillhando/app"
assets = root / "app/src/main/assets"
ui = json.loads((assets / "ui-en.json").read_text(encoding="utf-8"))
catalogue = json.loads((assets / "catalogue.json").read_text(encoding="utf-8"))
refs = {key for path in java.glob("*.kt")
        for key in re.findall(r'\btrf?\("([a-z0-9_]+)"', path.read_text(encoding="utf-8"))}
assert not refs - set(ui), sorted(refs - set(ui))
ids = [skill["id"] + "-" + challenge["id"] for skill in catalogue for challenge in skill["challenges"]]
assert len(catalogue) == 10 and len(ids) == 21 and len(ids) == len(set(ids))
assert all(len(skill["rubric"]) >= 3 and all(len(c["steps"]) >= 3 and c["criterion"].strip()
           and c["experiment"].strip() for c in skill["challenges"]) for skill in catalogue)
print(f"{len(refs)} interface references and {len(ids)} unique challenges validated.")
