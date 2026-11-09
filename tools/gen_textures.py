"""Generates the mod's small pixel-art textures. Run from the repo root: python3 tools/gen_textures.py"""
import math
import os
from PIL import Image

ROOT = os.path.join("src", "main", "resources", "assets", "curtaindoors", "textures")


def save(img, path):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    print("wrote", full)


def plastic_strip():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15)
            streak = 18 if x in (4, 5, 10) else 0
            a = 150 if edge else 62 + streak
            c = (235, 245, 250) if edge else (214, 232, 242)
            img.putpixel((x, y), (*c, a))
    return img


def bead():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 7.6:
                # Lit from the top-left, white so it can be tinted.
                lx, ly = x - 5.0, y - 5.0
                hl = max(0.0, 1.0 - math.hypot(lx, ly) / 4.0)
                shade = 0.72 + 0.28 * (1.0 - d / 7.6)
                v = min(255, int(255 * shade + 60 * hl))
                img.putpixel((x, y), (v, v, v, 255))
    return img


def curtain_cloth():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Two panels hanging from the rod, slightly parted at the bottom.
    for y in range(3, 16):
        gap = 0 if y < 11 else (y - 10)
        for x in range(2, 14):
            if 8 - gap // 2 - (gap % 2) <= x < 8 + gap // 2 and gap:
                continue
            fold = (x % 3)
            v = [235, 205, 180][fold]
            if y == 15:
                v -= 25
            img.putpixel((x, y), (v, v, v, 255))
    return img


def curtain_rod():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(1, 15):
        img.putpixel((x, 1), (156, 112, 66, 255))
        img.putpixel((x, 2), (112, 78, 44, 255))
    for x in (0, 15):
        img.putpixel((x, 1), (90, 62, 34, 255))
        img.putpixel((x, 2), (90, 62, 34, 255))
    # Rings holding the cloth.
    for x in (2, 5, 8, 10, 13):
        img.putpixel((x, 3), (170, 170, 170, 255))
    return img


def beaded_beads():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for col, x in enumerate((2, 5, 8, 11, 13)):
        for y in range(4 + (col % 2), 16, 2):
            img.putpixel((x, y), (240, 240, 240, 255) if y % 4 else (200, 200, 200, 255))
    return img


def beaded_strings():
    img = curtain_rod()
    for x in (2, 5, 8, 11, 13):
        img.putpixel((x, 3), (60, 50, 44, 255))
    return img


def plastic_item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(1, 15):
        img.putpixel((x, 1), (220, 220, 225, 255))
        img.putpixel((x, 2), (150, 150, 158, 255))
    for x in range(1, 15):
        strip_edge = x in (1, 4, 5, 8, 9, 11, 12, 14)
        for y in range(3, 16):
            a = 200 if strip_edge else 110
            img.putpixel((x, y), (205, 228, 240, a))
    return img


if __name__ == "__main__":
    save(plastic_strip(), "block/plastic_strip.png")
    save(bead(), "block/bead.png")
    save(curtain_cloth(), "item/curtain_cloth.png")
    save(curtain_rod(), "item/curtain_rod.png")
    save(beaded_beads(), "item/beaded_curtain_beads.png")
    save(beaded_strings(), "item/beaded_curtain_strings.png")
    save(plastic_item(), "item/plastic_strip_curtain.png")
