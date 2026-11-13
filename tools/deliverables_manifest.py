import csv
import hashlib
from fnmatch import fnmatch
from pathlib import Path

root = Path(__file__).resolve().parents[1]
folder = root / "deliverables"
manifest = folder / "DELIVERABLES_MANIFEST.csv"
ignored = [line[len("deliverables/"):] for line in (root / ".gitignore").read_text(encoding="utf-8").splitlines()
           if line.startswith("deliverables/")]
with manifest.open("w", encoding="utf-8", newline="") as stream:
    rows = csv.writer(stream)
    rows.writerow(("file", "bytes", "sha256"))
    for path in sorted(p for p in folder.rglob("*") if p.is_file() and p != manifest):
        if any(fnmatch(path.relative_to(folder).as_posix(), pattern) for pattern in ignored):
            continue
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        rows.writerow((path.relative_to(folder).as_posix(), path.stat().st_size, digest))
