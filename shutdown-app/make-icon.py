#!/usr/bin/env python3
"""生成关机 APK 的启动图标：深灰底 + 白色电源符号（圆环缺口 + 竖线）。"""
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "res", "mipmap-xxxhdpi")
os.makedirs(OUT, exist_ok=True)
os.makedirs(os.path.join(os.path.dirname(os.path.abspath(__file__)), "res", "drawable"), exist_ok=True)
SIZE = 192
BG = (31, 41, 55, 255)   # 深灰
FG = (255, 255, 255, 255)


def power_symbol(d, cx, cy, r, w):
    # 圆环：缺口朝上（12 点方向留约 60 度缺口），竖线从顶部穿过缺口
    box = [cx - r, cy - r, cx + r, cy + r]
    d.arc(box, start=300, end=240, fill=FG, width=w)
    d.line([(cx, cy - r - 8), (cx, cy - 6)], fill=FG, width=w)


# 电视横幅（320x180，供 Leanback 启动器当磁贴用）：电源符号 + “关机”二字
from PIL import ImageFont
banner = Image.new("RGBA", (320, 180), BG)
db = ImageDraw.Draw(banner)
power_symbol(db, 62, 90, 42, 11)
font = ImageFont.truetype("/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf", 62)
db.text((122, 58), "关机", font=font, fill=FG)
banner.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "res", "drawable", "banner.png"))
print("banner written")


# 方形图标（圆角）
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rounded_rectangle([4, 4, SIZE - 4, SIZE - 4], radius=42, fill=BG)
power_symbol(d, SIZE // 2, SIZE // 2 + 6, 52, 13)
img.save(os.path.join(OUT, "ic_launcher.png"))

# 圆形图标
img2 = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d2 = ImageDraw.Draw(img2)
d2.ellipse([4, 4, SIZE - 4, SIZE - 4], fill=BG)
power_symbol(d2, SIZE // 2, SIZE // 2 + 6, 52, 13)
img2.save(os.path.join(OUT, "ic_launcher_round.png"))

print("icons written to", OUT)
