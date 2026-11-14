"""Generate and validate development-time localisation drafts from public app copy."""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
OUT = ASSETS / "i18n"
REPORT = ROOT / "deliverables/localization"
LOCALES = ("en-GB en-US en-AU es-ES es-419 fr-FR fr-CA de-DE it-IT pt-BR pt-PT nl-NL pl-PL sv-SE ja-JP hi-IN ar tr-TR id-ID ko-KR zh-CN zh-TW").split()
PUBLIC = ("title", "description", "rubric", "objective", "materials", "steps", "criterion", "prompt", "experiment")
BRANDS = re.compile(r"SkillHando|Amazon|%[0-9]+\$[ds]|\{[a-z0-9_]+\}|\b(?:CSV|PDF)\b")


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def catalogue(obj, translate):
    if isinstance(obj, list):
        return [catalogue(item, translate) for item in obj]
    return {key: ([translate(item) for item in value] if isinstance(value, list) else translate(value))
            if key in PUBLIC else catalogue(value, translate) if key == "challenges" else value
            for key, value in obj.items()}


def validate(ui, content, target_ui, target_content):
    assert set(ui) == set(target_ui), "Missing or unexpected interface key"
    assert all(value.strip() and sorted(BRANDS.findall(ui[key])) == sorted(BRANDS.findall(value))
               for key, value in target_ui.items()), "Empty translation or damaged placeholder"
    def shape(rows):
        return [(skill["id"], len(skill["rubric"]), [(c["id"], c["minutes"], len(c["steps"])) for c in skill["challenges"]]) for skill in rows]
    assert shape(content) == shape(target_content), "Catalogue identifiers, instructions or timings changed"
    def copy_strings(rows):
        strings = []
        catalogue(rows, lambda text: strings.append(text) or text)
        return strings

    for source, target in zip(copy_strings(content), copy_strings(target_content), strict=True):
        assert target.strip() and sorted(BRANDS.findall(source)) == sorted(BRANDS.findall(target)), \
            f"Empty catalogue translation or damaged placeholder: {source}"


def run(args):
    ui, content = read(ASSETS / "ui-en.json"), read(ASSETS / "catalogue.json")
    total = set(ui.values())
    catalogue(content, lambda s: total.add(s) or s)
    report = {}
    cache_dir = ROOT / ".translation-cache"
    if args.model:
        import ctranslate2
        import sentencepiece
        model_path = Path(args.model)
        source_tokenizer = sentencepiece.SentencePieceProcessor(model_file=args.tokenizer)
        engine = ctranslate2.Translator(str(model_path), device="cpu", compute_type="int8",
                                       inter_threads=1, intra_threads=args.threads)
        fingerprint = hashlib.sha256((model_path / "config.json").read_bytes()).hexdigest()[:12]
    for tag in args.locale or LOCALES:
        lang = tag.split("-")[0]
        path = OUT / f"{tag}.json"
        content_path = OUT / f"catalogue-{tag}.json"
        if args.validate:
            if tag != "en-GB" and not (path.is_file() and content_path.is_file()):
                raise SystemExit(f"Missing interface or catalogue for {tag}")
        elif lang == "en":
            def regional(text):
                if tag != "en-US":
                    return text
                for old, new in (("practise", "practice"), ("organise", "organize"), ("recognised", "recognized"),
                                 ("localisation", "localization"), ("organising", "organizing"), ("personalised", "personalized")):
                    text = text.replace(old, new).replace(old.title(), new.title())
                return text
            write(path, {key: regional(value) for key, value in ui.items()})
            write(content_path, catalogue(content, regional))
        elif args.model:
            cache_path = cache_dir / f"{fingerprint}-{lang}.json"
            cache = read(cache_path) if cache_path.exists() else {}
            segments = {part.strip() for value in total for part in BRANDS.split(value) if re.search("[A-Za-z]", part)}
            pending = sorted(segments - set(cache))
            for offset in range(0, len(pending), 24):
                batch = pending[offset:offset+24]
                tokens = [["__en__", *source_tokenizer.encode(s, out_type=str), "</s>"] for s in batch]
                result = engine.translate_batch(tokens, target_prefix=[[f"__{lang}__"]] * len(batch),
                                                beam_size=1, max_decoding_length=150)
                for segment, translation in zip(batch, result):
                    decoded = source_tokenizer.decode([x for x in translation.hypotheses[0][1:] if x not in ("</s>", "<pad>")]).strip()
                    cache[segment] = decoded or segment
                write(cache_path, cache)
            def translate(text):
                parts, start = [], 0
                def apply(part):
                    bare = part.strip()
                    if bare not in cache:
                        return part
                    first = len(part) - len(part.lstrip())
                    end = len(part.rstrip())
                    return part[:first] + cache[bare] + part[end:]
                for match in BRANDS.finditer(text):
                    parts.extend((apply(text[start:match.start()]), match.group()))
                    start = match.end()
                return "".join(parts) + apply(text[start:])
            if lang == "zh":
                from opencc import OpenCC

                convert = OpenCC("s2twp" if tag == "zh-TW" else "t2s").convert
                translate_draft = translate

                def translate(text):
                    return convert(translate_draft(text))
            candidate_ui = {key: translate(value) for key, value in ui.items()}
            candidate_content = catalogue(content, translate)
            validate(ui, content, candidate_ui, candidate_content)
            write(path, candidate_ui)
            write(content_path, candidate_content)
        else:
            continue
        target_ui = read(path) if path.exists() else ui
        target_content = read(content_path) if content_path.exists() else content
        validate(ui, content, target_ui, target_content)
        report[tag] = {"status": "source" if tag == "en-GB" else "draft_pending_native_review", "interface": len(target_ui),
                       "changed": sum(target_ui[k] != ui[k] for k in ui), "skills": len(target_content),
                       "challenges": sum(len(item["challenges"]) for item in target_content)}
        print(tag, report[tag], flush=True)
    if args.validate:
        write(REPORT / "coverage.json", report)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--validate", action="store_true")
    parser.add_argument("--locale", choices=LOCALES, action="append")
    parser.add_argument("--model")
    parser.add_argument("--tokenizer")
    parser.add_argument("--threads", type=int, default=3)
    run(parser.parse_args())
