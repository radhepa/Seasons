"""Draws the season clock's four faces and the mod icon (palette-locked 16x16 pixel art).

    python tools/make_textures.py

Writes src/main/resources/assets/turningseasons/textures/item/season_clock_<season>.png and
src/main/resources/assets/turningseasons/icon.png. Edit the grids below and rerun; never edit the PNGs.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/turningseasons"

# The clock: a dark outline, a gold rim with a highlight, and a season-coloured face.
OUTLINE, RIM, RIM_LIGHT, RIM_DARK = (59, 42, 14), (226, 170, 52), (255, 226, 122), (170, 112, 28)

PALETTE = {
    "g": (74, 150, 52), "G": (122, 196, 76),          # leaf greens
    "p": (226, 120, 170), "P": (255, 178, 214),       # blossom pinks
    "Y": (255, 214, 64), "y": (255, 238, 140), "O": (255, 160, 32),   # sun
    "r": (168, 52, 28), "R": (214, 92, 34), "o": (242, 150, 48), "b": (110, 70, 34),  # autumn leaf
    "w": (232, 244, 255), "W": (255, 255, 255), "c": (150, 196, 236),  # snow
}

SEASONS = {
    "spring": ((196, 232, 170), [
        "...pp...",
        "..pPPp..",
        ".pPYPPp.",
        "..pPPp..",
        "...pp...",
        "....g.G.",
        "..Gg.gG.",
        "...gg...",
    ]),
    "summer": ((150, 212, 255), [
        "...yy...",
        ".y.YY.y.",
        "..YOOY..",
        "yYOOOOYy",
        "yYOOOOYy",
        "..YOOY..",
        ".y.YY.y.",
        "...yy...",
    ]),
    "autumn": ((246, 214, 160), [
        "...r....",
        "..rRr.r.",
        ".rRoRrR.",
        "rRoooRr.",
        ".rRooR..",
        "..rRR...",
        "...b....",
        "..b.....",
    ]),
    "winter": ((58, 90, 140), [
        "...w....",
        ".w.c.w..",
        "..wcw...",
        "wccWccw.",
        "..wcw...",
        ".w.c.w..",
        "...w....",
        "........",
    ]),
}


def clock(face, emblem):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.7:
                continue
            if d > 6.8:
                px[x, y] = OUTLINE + (255,)
            elif d > 5.6:
                # light from the top-left
                px[x, y] = (RIM_LIGHT if x + y < 12 else RIM_DARK if x + y > 18 else RIM) + (255,)
            else:
                px[x, y] = face + (255,)
    for j, row in enumerate(emblem):
        for i, ch in enumerate(row):
            if ch != ".":
                px[4 + i, 4 + j] = PALETTE[ch] + (255,)
    return img


def main():
    out = ASSETS / "textures/item"
    out.mkdir(parents=True, exist_ok=True)
    faces = []
    for name, (face, emblem) in SEASONS.items():
        img = clock(face, emblem)
        img.save(out / f"season_clock_{name}.png")
        faces.append(img)
    icon = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for k, img in enumerate(faces):
        icon.paste(img.resize((64, 64), Image.NEAREST), ((k % 2) * 64, (k // 2) * 64))
    icon.save(ASSETS / "icon.png")
    print("wrote", len(faces), "clock faces and icon.png")


if __name__ == "__main__":
    main()
