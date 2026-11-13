"""Generate draft store listings for documented Amazon storefront locale options."""
import argparse
from pathlib import Path
import re
from localize import ROOT, read, write, BRANDS

DEST = ROOT / "deliverables/store-assets/metadata"
TARGETS = "en-US en-GB en-AU en-IN de-DE es-ES es-MX fr-FR it-IT ja-JP nl-NL pl-PL pt-BR pt-PT sv-SE".split()
FIELDS = ("appTitle", "shortDescription", "longDescription", "featureBullets", "keywords", "releaseNotes", "screenshotHeadlines")


def generate(model, tokenizer):
    import ctranslate2
    import sentencepiece
    engine = ctranslate2.Translator(str(model), device="cpu", compute_type="int8", intra_threads=2)
    sp = sentencepiece.SentencePieceProcessor(model_file=str(tokenizer))
    source = read(DEST / "en-GB.json")
    originals = set()
    for field in FIELDS:
        strings = source[field] if isinstance(source[field], list) else [source[field]]
        originals.update(strings)
    result_cache = {}
    for tag in TARGETS:
        lang = tag.split("-")[0]
        if tag == "en-GB":
            continue
        if lang == "en":
            def translate(value):
                if tag != "en-US":
                    return value
                return value.replace("practise", "practice").replace("Practise", "Practice").replace("organise", "organize")
        else:
            if lang not in result_cache:
                pieces = sorted({piece.strip() for value in originals for piece in BRANDS.split(value) if re.search("[A-Za-z]", piece)})
                sentences = [["__en__", *sp.encode(piece, out_type=str), "</s>"] for piece in pieces]
                translated = engine.translate_batch(sentences, target_prefix=[[f"__{lang}__"]] * len(sentences),
                                                    beam_size=2, max_batch_size=8, max_decoding_length=300)
                result_cache[lang] = {
                    piece: sp.decode([token for token in item.hypotheses[0][1:] if token != "</s>"]).strip()
                    for piece, item in zip(pieces, translated)
                }
            def translate(value):
                index = 0
                output = []
                def segment(raw):
                    inner = raw.strip()
                    if inner not in result_cache[lang]:
                        return raw
                    return raw[:len(raw)-len(raw.lstrip())] + result_cache[lang][inner] + raw[len(raw.rstrip()):]
                for match in BRANDS.finditer(value):
                    output.extend((segment(value[index:match.start()]), match.group()))
                    index = match.end()
                return "".join(output) + segment(value[index:])
        listing = {field: [translate(s) for s in source[field]] if isinstance(source[field], list)
                   else translate(source[field]) for field in FIELDS}
        assert len(listing["shortDescription"].encode()) <= 2000 and len(listing["longDescription"]) <= 4000
        assert len(listing["featureBullets"]) in range(3, 6)
        assert len(listing["screenshotHeadlines"]) == 8
        listing["status"] = "Draft pending native review and store verification"
        write(DEST / f"{tag}.json", listing)
        print(tag, flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--tokenizer", type=Path, required=True)
    options = parser.parse_args()
    generate(options.model, options.tokenizer)
