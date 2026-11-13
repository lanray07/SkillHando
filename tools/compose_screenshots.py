"""Compose store drafts from actual device screen captures, with aspect ratio preserved."""
import argparse
import csv
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1] / "deliverables/store-assets"
SCREENS = ("today", "challenge", "coach", "voice", "comparison", "portfolio", "skills", "goals")


def render(capture, headline, output, font_path):
    with Image.open(capture) as source:
        image = source.convert("RGB")
    portrait = image.height >= image.width
    size = (1080, 1920) if portrait else (1920, 1080)
    canvas = Image.new("RGB", size, "#303A72")
    draw = ImageDraw.Draw(canvas)
    font_size = 60
    font = ImageFont.truetype(font_path, font_size)
    while font.getlength(headline) > size[0] - 110 and font_size > 26:
        font_size -= 2
        font = ImageFont.truetype(font_path, font_size)
    draw.text((size[0] / 2, 86), headline, anchor="mm", font=font, fill="#FAF8F2")
    image.thumbnail((size[0] - 72, size[1] - 175), Image.Resampling.LANCZOS)
    left, top = (size[0] - image.width) // 2, 140 + (size[1] - 175 - image.height) // 2
    draw.rounded_rectangle((left - 7, top - 7, left + image.width + 7, top + image.height + 7), radius=14, fill="#222A58")
    canvas.paste(image, (left, top))
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output)
    return size


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("captures", type=Path)
    parser.add_argument("--locale", default="en-GB")
    parser.add_argument("--device", default="fire-hd10")
    parser.add_argument("--font", default="C:/Windows/Fonts/georgia.ttf")
    options = parser.parse_args()
    metadata = json.loads((ROOT / "metadata" / f"{options.locale}.json").read_text(encoding="utf-8"))
    output = ROOT / "screenshots" / options.locale / options.device
    rows = []
    for number, (screen, headline) in enumerate(zip(SCREENS, metadata["screenshotHeadlines"], strict=True), 1):
        source = options.captures / f"{screen}.png"
        if not source.is_file():
            continue
        target = output / f"{number:02}-{screen}.png"
        width, height = render(source, headline, target, options.font)
        rows.append((options.locale, options.device, screen, target.name, width, height))
    if not rows:
        raise SystemExit("No device captures found")
    with (output / "manifest.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(("locale", "device", "screen", "image", "width", "height"))
        writer.writerows(rows)
    print(f"{len(rows)}/{len(SCREENS)} store drafts generated for {options.locale} / {options.device}.")
