"""Check package contents and the expected degree of release configuration."""
from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parents[1]
loc = json.loads((root / "deliverables/localization/coverage.json").read_text(encoding="utf-8"))
apks = [
    root / "app/build/outputs/apk/debug/app-debug.apk",
    root / "app/build/outputs/apk/release/app-release-unsigned.apk",
]
for apk in apks:
    with zipfile.ZipFile(apk) as package:
        files = set(package.namelist())
        assert len([name for name in files if name.startswith("classes") and name.endswith(".dex")]) >= 1
        assert "AndroidManifest.xml" in files
        assert "assets/AppstoreAuthenticationKey.pem" not in files
        assert all(f"assets/i18n/{tag}.json" in files and f"assets/i18n/catalogue-{tag}.json" in files for tag in loc)
        assert len(json.loads(package.read("assets/catalogue.json"))) == 10
        assert package.testzip() is None
    checksum = hashlib.sha256(apk.read_bytes()).hexdigest()
    print(f"{apk.name}\t{apk.stat().st_size}\t{checksum}\t{len(loc)} locale dictionaries and catalogues")
