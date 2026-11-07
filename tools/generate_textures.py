#!/usr/bin/env python3
"""Generates the detailed 32x32 machine and ore block textures plus the
16x16 ingot item textures for Nexus Tech Machines.

Deterministic (seeded RNG per texture) so CI commits stay stable.
Run from the repository root: python tools/generate_textures.py
Writes machine, ore and reactor PNGs to src/main/resources/assets/nexustech/textures/blocks/
and ingot and part PNGs to src/main/resources/assets/nexustech/textures/items/.
"""
import os
import random
import struct
import zlib

SIZE = 32
ITEM_SIZE = 16
OUT = os.path.join('src', 'main', 'resources', 'assets', 'nexustech',
                   'textures', 'blocks')
OUT_ITEMS = os.path.join('src', 'main', 'resources', 'assets', 'nexustech',
                         'textures', 'items')


def chunk(tag, data):
    return (struct.pack('>I', len(data)) + tag + data +
            struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff))


def png_bytes(pixels, size, color_type):
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for p in row:
            raw.extend(bytes(p))
    ihdr = struct.pack('>IIBBBBB', size, size, 8, color_type, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) +
            chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))


def png(pixels):
    return png_bytes(pixels, SIZE, 2)


def png_item(pixels):
    return png_bytes(pixels, ITEM_SIZE, 6)


class Tex(object):
    def __init__(self, seed, size=SIZE):
        self.size = size
        self.px = [[(0, 0, 0)] * size for _ in range(size)]
        self.rand = random.Random(seed)

    def set(self, x, y, c):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[y][x] = c

    def fill(self, c):
        for y in range(self.size):
            for x in range(self.size):
                self.px[y][x] = c

    def speckle(self, dark, light, chance=0.30):
        for y in range(self.size):
            for x in range(self.size):
                r = self.rand.random()
                if r < chance * 0.5:
                    self.px[y][x] = dark
                elif r < chance:
                    self.px[y][x] = light

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def border(self, c, t=1):
        self.rect(0, 0, self.size - 1, t - 1, c)
        self.rect(0, self.size - t, self.size - 1, self.size - 1, c)
        self.rect(0, 0, t - 1, self.size - 1, c)
        self.rect(self.size - t, 0, self.size - 1, self.size - 1, c)

    def hline(self, x0, x1, y, c):
        for x in range(x0, x1 + 1):
            self.set(x, y, c)

    def vline(self, x, y0, y1, c):
        for y in range(y0, y1 + 1):
            self.set(x, y, c)

    def frame(self, x0, y0, x1, y1, c):
        self.hline(x0, x1, y0, c)
        self.hline(x0, x1, y1, c)
        self.vline(x0, y0, y1, c)
        self.vline(x1, y0, y1, c)

    def rivet(self, x, y):
        self.rect(x, y, x + 1, y + 1, (88, 96, 104))
        self.set(x, y, (122, 132, 141))

    def corner_rivets(self):
        self.rivet(2, 2)
        self.rivet(self.size - 4, 2)
        self.rivet(2, self.size - 4)
        self.rivet(self.size - 4, self.size - 4)


IRON = (58, 63, 70)
IRON_D = (34, 38, 43)
IRON_L = (74, 80, 88)
STEEL = (74, 80, 88)


def machine_base(t, base=IRON):
    t.fill(base)
    t.speckle((49, 54, 60), (67, 73, 81))
    t.border(IRON_D, 2)


def coal_generator_side(seed):
    t = Tex(seed)
    machine_base(t)
    for y0 in (8, 15, 22):
        t.hline(5, 26, y0, (28, 32, 37))
        t.hline(5, 26, y0 + 1, IRON_L)
        t.hline(5, 26, y0 + 2, (42, 47, 53))
    t.corner_rivets()
    return t


def coal_generator_front(seed):
    t = Tex(seed)
    machine_base(t)
    t.rect(7, 8, 24, 23, (30, 33, 38))
    t.frame(7, 8, 24, 23, (88, 96, 104))
    for x in range(9, 23):
        t.set(x, 21, (255, 140, 26) if x % 3 else (200, 90, 18))
        if x % 2:
            t.set(x, 20, (255, 179, 71))
        if x % 4 == 1:
            t.set(x, 19, (255, 110, 20))
    for (x, y) in ((9, 18), (12, 19), (18, 19), (21, 18)):
        t.set(x, y, (20, 22, 26))
    t.rect(14, 10, 17, 11, (122, 132, 141))
    t.corner_rivets()
    return t


def coal_generator_top(seed):
    t = Tex(seed)
    machine_base(t)
    t.rect(5, 5, 26, 26, (42, 47, 53))
    t.frame(5, 5, 26, 26, (88, 96, 104))
    t.hline(12, 19, 15, (122, 132, 141))
    t.hline(12, 19, 16, IRON_L)
    t.rivet(6, 6)
    t.rivet(25, 6)
    t.rivet(6, 25)
    t.rivet(25, 25)
    t.corner_rivets()
    return t


def solar_panel_top(seed):
    t = Tex(seed)
    t.fill((43, 47, 54))
    t.speckle((36, 40, 46), (52, 57, 64))
    t.border((27, 31, 36), 2)
    t.rect(3, 3, 28, 28, (11, 58, 102))
    for x in range(3, 29):
        if x % 8 == 3:
            t.vline(x, 3, 28, (16, 63, 110))
    for y in range(3, 29):
        if y % 8 == 3:
            t.hline(3, 28, y, (16, 63, 110))
    for i in range(20):
        x = 4 + i
        y = 26 - i
        if x <= 27 and y >= 4:
            t.set(x, y, (49, 127, 196))
    for (x, y) in ((6, 6), (10, 6), (14, 6), (6, 10)):
        t.set(x, y, (62, 144, 209))
    for (x, y) in ((1, 1), (29, 1), (1, 29), (29, 29)):
        t.set(x, y, (122, 132, 141))
    return t


def solar_panel_side(seed):
    t = Tex(seed)
    t.fill((27, 31, 36))
    t.hline(0, 31, 0, (43, 47, 54))
    t.hline(0, 31, 1, (43, 47, 54))
    t.hline(0, 31, 2, (43, 47, 54))
    t.hline(0, 31, 3, (43, 47, 54))
    t.hline(0, 31, 4, (16, 19, 22))
    t.rect(5, 5, 31, 31, (20, 23, 27))
    t.rect(14, 20, 17, 23, IRON)
    t.set(15, 21, (124, 86, 40))
    t.rivet(2, 6)
    t.rivet(28, 6)
    return t


def energy_cable(seed):
    t = Tex(seed)
    t.fill((24, 27, 31))
    t.speckle((19, 22, 25), (30, 34, 39))
    for y in range(SIZE):
        for x in range(SIZE):
            d = max(abs(x - 15), abs(y - 15))
            if d in (8, 9):
                t.set(x, y, (52, 59, 68))
            elif d in (6, 7):
                t.set(x, y, (36, 41, 48))
    for y in range(11, 21):
        for x in range(11, 21):
            d = max(abs(x - 15), abs(y - 15))
            t.set(x, y, (140, 84, 40) if d >= 2 else (184, 115, 51))
    t.set(13, 13, (220, 160, 90))
    t.set(14, 13, (220, 160, 90))
    t.set(13, 14, (220, 160, 90))
    t.border((19, 22, 25), 1)
    return t


def electric_furnace_front(seed):
    t = Tex(seed)
    machine_base(t, base=STEEL)
    for y in (3, 5):
        t.hline(4, 27, y, (28, 32, 37))
        t.hline(4, 27, y + 1, IRON)
    t.rect(6, 10, 25, 25, (30, 33, 38))
    t.frame(6, 10, 25, 25, (88, 96, 104))
    for y in range(12, 24):
        heat = 1.0 - (24 - y) / 12.0
        bright = (255, 90 + int(80 * heat), 31 + int(40 * heat))
        dark = (194, 65, 14)
        for x in range(8, 24):
            if (x // 2) % 2 == 0:
                t.set(x, y, bright if y % 2 == 0 else dark)
    t.rect(14, 19, 17, 22, (255, 196, 90))
    t.corner_rivets()
    return t


def electric_furnace_side(seed):
    t = Tex(seed)
    machine_base(t, base=STEEL)
    for x in (6, 12, 18, 24):
        t.vline(x, 6, 25, (28, 32, 37))
        t.vline(x + 1, 6, 25, IRON)
    t.hline(4, 27, 27, (28, 32, 37))
    t.corner_rivets()
    return t


def electric_furnace_top(seed):
    t = Tex(seed)
    machine_base(t, base=STEEL)
    cx = cy = 15
    for y in range(SIZE):
        for x in range(SIZE):
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            if 64 <= d2 <= 100:
                t.set(x, y, (30, 33, 38))
    for i in range(3, 9):
        t.set(cx + i, cy, IRON)
        t.set(cx - i, cy, IRON)
        t.set(cx, cy + i, IRON)
        t.set(cx, cy - i, IRON)
    t.rect(cx - 1, cy - 1, cx + 1, cy + 1, (122, 132, 141))
    t.corner_rivets()
    return t


def electric_anvil_top(seed):
    t = Tex(seed)
    t.fill((107, 116, 128))
    t.speckle((96, 104, 115), (120, 130, 142))
    t.border(IRON, 2)
    points = [(4, 4), (7, 4), (7, 7), (11, 7), (11, 11), (14, 11), (14, 14),
              (18, 14), (18, 18), (21, 18), (21, 21), (25, 21), (25, 25),
              (28, 25)]
    for (x, y) in points:
        t.set(x, y, (142, 230, 255))
    t.rivet(2, 2)
    t.rivet(28, 2)
    t.rivet(2, 28)
    t.rivet(28, 28)
    return t


def electric_anvil_side(seed):
    t = Tex(seed)
    t.fill((27, 31, 36))
    t.rect(2, 4, 29, 8, (86, 94, 102))
    t.hline(2, 29, 4, (122, 132, 141))
    t.hline(2, 29, 8, IRON)
    t.rect(11, 9, 20, 17, (86, 94, 102))
    t.vline(11, 9, 17, (122, 132, 141))
    t.vline(20, 9, 17, IRON)
    t.rect(5, 18, 26, 21, (74, 80, 88))
    t.rect(3, 22, 28, 27, (86, 94, 102))
    t.hline(3, 28, 22, (122, 132, 141))
    t.hline(3, 28, 27, (50, 55, 61))
    t.set(15, 13, (142, 230, 255))
    t.set(16, 13, (142, 230, 255))
    t.set(15, 12, (94, 196, 255))
    return t


def electric_anvil_front(seed):
    t = Tex(seed)
    t.fill((86, 94, 102))
    t.speckle((74, 80, 88), (107, 116, 128))
    t.border((50, 55, 61), 2)
    for i in range(6, 26):
        t.set(i, i, IRON)
        t.set(31 - i, i, IRON)
    t.rect(13, 13, 18, 18, (30, 33, 38))
    t.rect(15, 15, 16, 16, (142, 230, 255))
    t.rivet(2, 2)
    t.rivet(28, 2)
    t.rivet(2, 28)
    t.rivet(28, 28)
    return t


def digital_mine_front(seed):
    t = Tex(seed)
    t.fill((43, 48, 56))
    t.speckle((37, 42, 49), (52, 58, 66))
    t.border((23, 27, 32), 2)
    t.rect(4, 4, 27, 17, (4, 34, 15))
    t.frame(4, 4, 27, 17, IRON_L)
    t.rect(13, 7, 18, 9, (74, 222, 128))
    t.rect(14, 10, 17, 11, (52, 211, 153))
    t.rect(15, 12, 16, 13, (34, 197, 94))
    t.hline(6, 25, 15, (22, 101, 52))
    for i in range(6):
        x = 5 + i * 4
        t.rect(x, 20, x + 1, 21, (74, 222, 128) if i < 4 else (16, 20, 24))
    for y in (26, 28):
        t.hline(4, 27, y, (23, 27, 32))
        t.hline(4, 27, y + 1, IRON_L)
    return t


def digital_mine_side(seed):
    t = Tex(seed)
    t.fill((43, 48, 56))
    t.speckle((37, 42, 49), (52, 58, 66))
    t.border((23, 27, 32), 2)
    for y in (8, 16, 24):
        t.hline(2, 29, y, (23, 27, 32))
        t.hline(2, 29, y + 1, IRON_L)
    for y in (4, 12, 20, 28):
        t.rivet(3, y)
        t.rivet(26, y)
    t.hline(10, 18, 5, IRON_L)
    return t


def digital_mine_top(seed):
    t = Tex(seed)
    t.fill((35, 40, 47))
    t.speckle((29, 34, 40), (43, 48, 56))
    t.border((23, 27, 32), 2)
    cx = cy = 15
    for y in range(SIZE):
        for x in range(SIZE):
            d = max(abs(x - cx), abs(y - cy))
            if d == 6:
                t.set(x, y, (23, 27, 32))
            elif d == 5:
                t.set(x, y, IRON_L)
            elif d == 4:
                t.set(x, y, (52, 58, 66))
    t.rect(cx - 3, cy - 3, cx + 3, cy + 3, (140, 84, 40))
    t.set(cx - 1, cy - 1, (220, 160, 90))
    t.hline(cx - 2, cx + 1, cy - 2, (184, 115, 51))
    t.vline(cx + 2, cy - 2, cy + 2, (110, 66, 32))
    t.rivet(2, 2)
    t.rivet(28, 2)
    t.rivet(2, 28)
    t.rivet(28, 28)
    t.set(27, 5, (74, 222, 128))
    return t


TEXTURES = {
    'coal_generator_side': coal_generator_side,
    'coal_generator_front': coal_generator_front,
    'coal_generator_top': coal_generator_top,
    'solar_panel_top': solar_panel_top,
    'solar_panel_side': solar_panel_side,
    'energy_cable': energy_cable,
    'electric_furnace_front': electric_furnace_front,
    'electric_furnace_side': electric_furnace_side,
    'electric_furnace_top': electric_furnace_top,
    'electric_anvil_front': electric_anvil_front,
    'electric_anvil_side': electric_anvil_side,
    'electric_anvil_top': electric_anvil_top,
    'digital_mine_front': digital_mine_front,
    'digital_mine_side': digital_mine_side,
    'digital_mine_top': digital_mine_top,
}


STONE = (125, 125, 125)
STONE_D = (100, 100, 100)
STONE_L = (143, 143, 143)

ORE_COLORS = {
    'copper': ((132, 76, 36), (186, 113, 52), (224, 158, 88)),
    'tin': ((146, 158, 172), (198, 208, 218), (234, 240, 244)),
    'aluminum': ((160, 167, 173), (216, 222, 227), (247, 249, 251)),
    'lead': ((80, 94, 112), (128, 143, 162), (173, 187, 203)),
    'uranium': ((30, 78, 30), (74, 152, 58), (118, 208, 92)),
}

INGOT_COLORS = {
    'copper': ((163, 95, 40), (214, 140, 60), (243, 192, 124), (70, 40, 18)),
    'tin': ((150, 164, 176), (205, 216, 225), (238, 244, 249), (92, 102, 113)),
    'aluminum': ((165, 172, 178), (222, 228, 233), (251, 253, 255), (98, 104, 110)),
    'lead': ((96, 110, 127), (144, 157, 172), (196, 206, 218), (46, 54, 64)),
    'uranium': ((56, 118, 48), (96, 178, 74), (156, 226, 124), (22, 46, 20)),
}

METALS = ('copper', 'tin', 'aluminum', 'lead', 'uranium')


def ore_base(t):
    t.fill(STONE)
    t.speckle(STONE_D, STONE_L, 0.38)


def ore_blob(t, x, y, dark, mid, light):
    t.set(x + 1, y, light)
    t.set(x + 2, y, light)
    t.set(x, y + 1, light)
    t.set(x + 1, y + 1, mid)
    t.set(x + 2, y + 1, mid)
    t.set(x, y + 2, mid)
    t.set(x + 1, y + 2, mid)
    t.set(x + 2, y + 2, dark)
    t.set(x + 3, y + 2, dark)
    t.set(x + 1, y + 3, mid)
    t.set(x + 2, y + 3, dark)
    t.set(x + 3, y + 3, dark)


def ore_dot(t, x, y, dark, mid, light):
    t.set(x, y, light)
    t.set(x + 1, y, mid)
    t.set(x, y + 1, mid)
    t.set(x + 1, y + 1, dark)


def ore_texture(seed, metal):
    dark, mid, light = ORE_COLORS[metal]
    t = Tex(seed)
    ore_base(t)
    for (x, y) in ((4, 3), (21, 5), (3, 18), (23, 21), (13, 12)):
        ore_blob(t, x + t.rand.randint(-1, 1), y + t.rand.randint(-1, 1),
                 dark, mid, light)
    for (x, y) in ((9, 10), (17, 15), (8, 26), (19, 26), (28, 4), (15, 4)):
        ore_dot(t, x + t.rand.randint(-1, 1), y + t.rand.randint(-1, 1),
                dark, mid, light)
    return t


def rgba(c):
    return (c[0], c[1], c[2], 255)


def polyfill(t, pts, c):
    n = len(pts)
    for y in range(t.size):
        for x in range(t.size):
            px = x + 0.5
            py = y + 0.5
            inside = False
            j = n - 1
            for i in range(n):
                xi, yi = pts[i]
                xj, yj = pts[j]
                if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                    inside = not inside
                j = i
            if inside:
                t.set(x, y, c)


def ingot_texture(seed, metal):
    dark, mid, light, outline = INGOT_COLORS[metal]
    t = Tex(seed, ITEM_SIZE)
    t.fill((0, 0, 0, 0))
    polyfill(t, ((1, 6), (3, 9), (3, 13), (1, 10)), rgba(dark))
    polyfill(t, ((3, 9), (13, 7), (13, 11), (3, 13)), rgba(mid))
    polyfill(t, ((1, 6), (11, 4), (13, 7), (3, 9)), rgba(light))
    for y in range(ITEM_SIZE):
        for x in range(ITEM_SIZE):
            if t.px[y][x][3] == 0:
                continue
            for (dx, dy) in ((1, 0), (0, 1)):
                nx = x + dx
                ny = y + dy
                if nx >= ITEM_SIZE or ny >= ITEM_SIZE or t.px[ny][nx][3] == 0:
                    t.px[y][x] = rgba(outline)
                    break
    t.set(3, 7, (255, 244, 230, 255))
    t.set(4, 6, (255, 244, 230, 255))
    return t


ORE_TEXTURES = {}
ITEM_TEXTURES = {}
for metal in METALS:
    ORE_TEXTURES[metal + '_ore'] = (lambda m: lambda seed: ore_texture(seed, m))(metal)
    ITEM_TEXTURES[metal + '_ingot'] = (lambda m: lambda seed: ingot_texture(seed, m))(metal)


YELLOW = (222, 168, 22)
HAZARD_DARK = (32, 32, 32)
URANIUM = (74, 152, 58)
URANIUM_L = (118, 208, 92)
URANIUM_D = (34, 88, 30)
VENT_SLAT = (110, 118, 127)
ROD_BODY = (105, 115, 126)
ROD_LIGHT = (150, 160, 170)
ROD_DARK = (70, 76, 84)


def hazard_band(t, y0, y1):
    for y in range(y0, y1 + 1):
        for x in range(SIZE):
            t.set(x, y, YELLOW if ((x + y) // 2) % 2 == 0 else HAZARD_DARK)


def nuclear_reactor_front(seed):
    t = Tex(seed)
    machine_base(t)
    t.rect(10, 11, 21, 20, IRON_D)
    for y in (13, 15, 17, 19):
        t.hline(11, 20, y, VENT_SLAT)
    t.rect(23, 6, 26, 9, IRON_D)
    t.rect(24, 7, 25, 8, URANIUM)
    t.set(24, 7, URANIUM_L)
    hazard_band(t, 24, 29)
    t.rivet(2, 2)
    t.rivet(28, 2)
    t.rivet(2, 21)
    t.rivet(28, 21)
    return t


def nuclear_reactor_side(seed):
    t = Tex(seed)
    machine_base(t)
    for x in (7, 23):
        t.rect(x, 5, x + 2, 21, IRON_D)
        t.vline(x, 5, 21, IRON_L)
        t.rect(x - 1, 5, x + 3, 5, IRON_D)
        t.rect(x - 1, 21, x + 3, 21, IRON_D)
    hazard_band(t, 24, 29)
    t.rivet(2, 2)
    t.rivet(28, 2)
    t.rivet(2, 21)
    t.rivet(28, 21)
    return t


def nuclear_reactor_top(seed):
    t = Tex(seed)
    machine_base(t)
    for (vx, vy) in ((6, 6), (17, 6), (6, 17), (17, 17)):
        t.rect(vx, vy, vx + 8, vy + 8, IRON_D)
        for i in range(2, 8, 2):
            t.hline(vx + 1, vx + 7, vy + i, VENT_SLAT)
            t.vline(vx + i, vy + 1, vy + 7, VENT_SLAT)
    return t


def uranium_rod(seed):
    t = Tex(seed, ITEM_SIZE)
    t.fill((0, 0, 0, 0))
    t.rect(6, 1, 9, 14, ROD_BODY)
    t.vline(6, 1, 14, ROD_LIGHT)
    t.rect(6, 1, 9, 2, ROD_DARK)
    t.rect(6, 13, 9, 14, ROD_DARK)
    for (y0, y1) in ((4, 5), (7, 8), (10, 11)):
        t.rect(7, y0, 8, y1, URANIUM)
        t.hline(7, 8, y0, URANIUM_L)
        t.hline(7, 8, y1, URANIUM_D)
    return t


def reactor_casing(seed):
    t = Tex(seed, ITEM_SIZE)
    t.fill(IRON)
    t.speckle((49, 54, 60), (67, 73, 81), 0.30)
    t.border(IRON_D, 1)
    t.rect(6, 4, 9, 7, IRON_D)
    t.rect(7, 5, 8, 6, URANIUM)
    t.set(7, 5, URANIUM_L)
    for y in range(12, 14):
        for x in range(ITEM_SIZE):
            t.set(x, y, YELLOW if ((x + y) // 2) % 2 == 0 else HAZARD_DARK)
    t.rivet(2, 2)
    t.rivet(12, 2)
    t.rivet(2, 12)
    t.rivet(12, 12)
    return t


REACTOR_TEXTURES = {
    'nuclear_reactor_front': nuclear_reactor_front,
    'nuclear_reactor_side': nuclear_reactor_side,
    'nuclear_reactor_top': nuclear_reactor_top,
}

PART_TEXTURES = {
    'uranium_rod': uranium_rod,
    'reactor_casing': reactor_casing,
}


def write_texture(directory, name, data):
    path = os.path.join(directory, name + '.png')
    with open(path, 'wb') as f:
        f.write(data)
    print('wrote ' + path)


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(OUT_ITEMS, exist_ok=True)
    for i, (name, paint) in enumerate(sorted(TEXTURES.items())):
        write_texture(OUT, name, png(paint(1000 + i).px))
    for i, (name, paint) in enumerate(sorted(ORE_TEXTURES.items())):
        write_texture(OUT, name, png(paint(2000 + i).px))
    for i, (name, paint) in enumerate(sorted(ITEM_TEXTURES.items())):
        write_texture(OUT_ITEMS, name, png_item(paint(3000 + i).px))
    for i, (name, paint) in enumerate(sorted(REACTOR_TEXTURES.items())):
        write_texture(OUT, name, png(paint(4000 + i).px))
    for i, (name, paint) in enumerate(sorted(PART_TEXTURES.items())):
        write_texture(OUT_ITEMS, name, png_item(paint(5000 + i).px))


if __name__ == '__main__':
    main()
