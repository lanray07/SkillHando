"""Validate screenshot packages and refresh the combined storefront manifest."""
import csv
import hashlib
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / "deliverables/store-assets"
FILES = ("today", "challenge", "coach", "voice", "comparison", "portfolio", "skills", "goals")
SIZES = {(1920, 1080), (1080, 1920)}
FIELDS = ("locale", "device", "screen", "image", "width", "height", "sha256")


def main():
    rows = []
    expected = {p.stem for p in (ROOT / "metadata").glob("*.json")}
    seen = set()
    for manifest in sorted((ROOT / "screenshots").glob("*/*/manifest.csv")):
        locale, device = manifest.parent.parent.name, manifest.parent.name
        assert locale in expected
        metadata = json.loads((ROOT / "metadata" / f"{locale}.json").read_text(encoding="utf-8"))
        assert len(metadata["screenshotHeadlines"]) == len(FILES)
        with manifest.open(encoding="utf-8", newline="") as stream:
            package = list(csv.DictReader(stream))
        assert [entry["screen"] for entry in package] == list(FILES), manifest
        raw_path = ROOT / "device-captures" / locale / device
        assert len({hashlib.sha256((raw_path / f"{name}.png").read_bytes()).digest() for name in FILES}) == len(FILES), raw_path
        for index, entry in enumerate(package, 1):
            image = manifest.parent / entry["image"]
            assert entry["image"] == f"{index:02}-{entry['screen']}.png"
            with Image.open(image) as rendered:
                assert rendered.format == "PNG" and rendered.mode == "RGB"
                assert rendered.size == (int(entry["width"]), int(entry["height"]))
                assert rendered.size in SIZES
                rendered.verify()
            rows.append(dict(entry, image=image.relative_to(ROOT / "screenshots").as_posix(),
                             sha256=hashlib.sha256(image.read_bytes()).hexdigest()))
        seen.add((locale, device))
    with (ROOT / "screenshots/manifest.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    for profile in ("fire-hd10", "phone-layout"):
        missing = sorted(expected - {locale for locale, device in seen if profile == device})
        print(f"{profile}: {len(expected) - len(missing)}/{len(expected)} storefront locales; pending {', '.join(missing) or 'none'}")
    print(f"{len(rows)} PNGs validated")


if __name__ == "__main__":
    main()
