"""Build the store icon from the source vector, preserving transparency."""
from pathlib import Path
from xml.etree import ElementTree as ET
from PIL import Image, ImageDraw
from aggdraw import Draw, Symbol, Brush, Pen

root = Path(__file__).resolve().parents[1]
vector = ET.parse(root / "app/src/main/res/drawable/ic_hando.xml").getroot()
ns = "{http://schemas.android.com/apk/res/android}"
icon = Image.new("RGBA", (1080, 1080))
ImageDraw.Draw(icon).rounded_rectangle((0, 0, 1079, 1079), 225, fill="#303A72")
mark = Image.new("RGBA", (108, 108))
draw = Draw(mark)
for node in vector:
    d = node.attrib
    data = d[ns + "pathData"].replace(",", " ")
    brush = Brush(d[ns + "fillColor"]) if ns + "fillColor" in d else None
    pen = Pen(d[ns + "strokeColor"], float(d[ns + "strokeWidth"])) if ns + "strokeColor" in d else None
    draw.symbol((0, 0), Symbol(data), brush, pen)
draw.flush()
icon.alpha_composite(mark.resize(icon.size, Image.Resampling.LANCZOS))
out = root / "deliverables/brand-assets"
out.mkdir(parents=True, exist_ok=True)
icon.resize((1024, 1024), Image.Resampling.LANCZOS).save(out / "skillhando-master.png")
icon.resize((512, 512), Image.Resampling.LANCZOS).save(out / "skillhando-amazon-icon.png")

icon.resize((114, 114), Image.Resampling.LANCZOS).save(out / "skillhando-amazon-small.png")
