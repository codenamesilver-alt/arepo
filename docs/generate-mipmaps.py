from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math
import random
import numpy as np
import os

rng = random.Random(42)

W, H = 1024, 1024
MARGIN = 50

GROUND = (246, 242, 233)
INDIGO_DARK = (26, 39, 68)
INDIGO_MID = (45, 74, 122)
INDIGO_LIGHT = (91, 127, 181)

img = Image.new("RGB", (W, H), GROUND)
draw = ImageDraw.Draw(img)

cx, cy = W // 2, H // 2 - 20
radius = 220

# --- Dense woven grid ---
for i in range(52):
    x = i * ((W - 2 * MARGIN) / 51) + MARGIN
    op = 0.20 + rng.random() * 0.30
    c = tuple(int(INDIGO_MID[j] * op + GROUND[j] * (1 - op)) for j in range(3))
    draw.line([(x, MARGIN), (x, H - MARGIN)], fill=c, width=2)

for i in range(52):
    y = i * ((H - 2 * MARGIN) / 51) + MARGIN
    op = 0.18 + rng.random() * 0.28
    c = tuple(int(INDIGO_LIGHT[j] * op + GROUND[j] * (1 - op)) for j in range(3))
    draw.line([(MARGIN, y), (W - MARGIN, y)], fill=c, width=2)

# --- Bold outer circle ---
draw.ellipse([cx - radius - 14, cy - radius - 14, cx + radius + 14, cy + radius + 14],
             outline=INDIGO_DARK, width=7)
draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
             fill=None, outline=INDIGO_DARK, width=3)

# --- Dense fill inside circle ---
for i in range(400):
    angle = rng.uniform(0, 2 * math.pi)
    dist = rng.uniform(10, radius - 8)
    x = cx + dist * math.cos(angle)
    y = cy + dist * math.sin(angle)
    if (x - cx) ** 2 + (y - cy) ** 2 < (radius - 3) ** 2:
        size = rng.uniform(1.0, 4.0)
        op = rng.uniform(0.15, 0.55)
        c = tuple(int(INDIGO_DARK[j] * op + GROUND[j] * (1 - op)) for j in range(3))
        draw.ellipse([x - size, y - size, x + size, y + size], fill=c)

# --- Concentric rings ---
for r in range(25, radius - 8, 12):
    op = 0.25 + (r / radius) * 0.40
    c = tuple(int(INDIGO_MID[j] * op + GROUND[j] * (1 - op)) for j in range(3))
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c, width=1)

# --- G letterform ---
g_r = 105
g_bbox = [cx - g_r, cy - g_r, cx + g_r, cy + g_r]
draw.arc(g_bbox, -60, 270, fill=INDIGO_DARK, width=18)
draw.line([(cx - 35, cy + 8), (cx + 60, cy + 8)], fill=INDIGO_DARK, width=18)

# --- Fine grid inside circle ---
for ix in range(-15, 16):
    for iy in range(-15, 16):
        x = cx + ix * 12
        y = cy + iy * 12
        if (x - cx) ** 2 + (y - cy) ** 2 < (radius - 25) ** 2:
            op = rng.uniform(0.05, 0.15)
            c = tuple(int(INDIGO_LIGHT[j] * op + GROUND[j] * (1 - op)) for j in range(3))
            draw.rectangle([x - 1, y - 1, x + 1, y + 1], fill=c)

# --- Cross marks outside circle ---
for i in range(100):
    x = rng.uniform(MARGIN + 15, W - MARGIN - 15)
    y = rng.uniform(MARGIN + 15, H - MARGIN - 15)
    if (x - cx) ** 2 + (y - cy) ** 2 > (radius + 60) ** 2:
        s = rng.uniform(2, 7)
        op = rng.uniform(0.10, 0.30)
        c = tuple(int(INDIGO_LIGHT[j] * op + GROUND[j] * (1 - op)) for j in range(3))
        draw.line([(x - s, y), (x + s, y)], fill=c, width=1)
        draw.line([(x, y - s), (x, y + s)], fill=c, width=1)

# --- Registration marks ---
mark_len = 12
for corner_x, corner_y in [(MARGIN + 15, MARGIN + 15), (W - MARGIN - 15, MARGIN + 15),
                             (MARGIN + 15, H - MARGIN - 15), (W - MARGIN - 15, H - MARGIN - 15)]:
    c = tuple(int(INDIGO_DARK[j] * 0.35 + GROUND[j] * 0.65) for j in range(3))
    draw.line([(corner_x - mark_len, corner_y), (corner_x + mark_len, corner_y)], fill=c, width=1)
    draw.line([(corner_x, corner_y - mark_len), (corner_x, corner_y + mark_len)], fill=c, width=1)

# --- Border ---
border_c = tuple(int(INDIGO_DARK[j] * 0.12 + GROUND[j] * 0.88) for j in range(3))
draw.rectangle([MARGIN + 5, MARGIN + 5, W - MARGIN - 5, H - MARGIN - 5], outline=border_c, width=1)

# --- Text area (get cropped in mipmap anyway, keep for full logo) ---
font_path = "C:/Users/SHAMSI/.config/opencode/skills/designing-canvas-art/canvas-fonts/Jura-Medium.ttf"
try:
    font_large = ImageFont.truetype(font_path, 34)
    font_small = ImageFont.truetype(font_path, 13)
except:
    font_large = ImageFont.load_default()
    font_small = ImageFont.load_default()

text_y = H - 95
text = "G A R M E N T O"
bbox = draw.textbbox((0, 0), text, font=font_large)
tw = bbox[2] - bbox[0]
draw.text(((W - tw) // 2, text_y), text, fill=INDIGO_DARK, font=font_large)

line_c = tuple(int(INDIGO_DARK[j] * 0.35 + GROUND[j] * 0.65) for j in range(3))
draw.line([(W // 2 - 160, text_y + 42), (W // 2 + 160, text_y + 42)], fill=line_c, width=1)

sub = "laundry subscription system"
bbox_s = draw.textbbox((0, 0), sub, font=font_small)
sw = bbox_s[2] - bbox_s[0]
sub_c = tuple(int(INDIGO_MID[j] * 0.45 + GROUND[j] * 0.55) for j in range(3))
draw.text(((W - sw) // 2, text_y + 50), sub, fill=sub_c, font=font_small)

# --- Grain ---
a = np.asarray(img, dtype=np.float32)
noise = np.random.default_rng(42).normal(0, 4, a.shape[:2])[..., None]
out = np.clip(a + noise, 0, 255).astype(np.uint8)
img = Image.fromarray(out)
img = img.filter(ImageFilter.GaussianBlur(0.25))

# Save full version
img.save("C:/Users/SHAMSI/AndroidStudioProjects/GARMENTO/docs/garmento-logo.png", dpi=(300, 300))

# --- Generate mipmap sizes ---
res_dir = "C:/Users/SHAMSI/AndroidStudioProjects/GARMENTO/app/src/main/res"
sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

for density, size in sizes.items():
    resized = img.resize((size, size), Image.LANCZOS)
    # For launcher icon, crop to the circular "G" portion: center square
    # Actually for legacy icons use the full design
    resized.save(f"{res_dir}/mipmap-{density}/ic_launcher.png")
    resized.save(f"{res_dir}/mipmap-{density}/ic_launcher_round.png")
    print(f"{density}: {size}x{size} saved")

print("All mipmaps generated")