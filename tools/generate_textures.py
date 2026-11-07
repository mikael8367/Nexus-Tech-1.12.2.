#!/usr/bin/env python3
"""Generates the detailed 32x32 machine textures for Nexus Tech Machines.

Deterministic (seeded RNG per texture) so CI commits stay stable.
Run from the repository root: python tools/generate_textures.py
Writes PNGs to src/main/resources/assets/nexustech/textures/blocks/.
"""
import os
import random
import struct
import zlib

SIZE = 32
OUT = os.path.join('src', 'main', 'resources', 'assets', 'nexustech',
                   'textures', 'blocks')


def chunk(tag, data):
    return (struct.pack('>I', len(data)) + tag + data +
            struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff))


def png(pixels):
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for p in row:
            raw.extend(bytes(p))
    ihdr = struct.pack('>IIBBBBB', SIZE, SIZE, 8, 2, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) +
            chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))


class Tex(object):
    def __init__(self, seed):
        self.px = [[(0, 0, 0)] * SIZE for _ in range(SIZE)]
        self.rand = random.Random(seed)

    def set(self, x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            self.px[y][x] = c

    def fill(self, c):
        for y in range(SIZE):
            for x in range(SIZE):
                self.px[y][x] = c

    def speckle(self, dark, light, chance=0.30):
        for y in range(SIZE):
            for x in range(SIZE):
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
        self.rect(0, 0, SIZE - 1, t - 1, c)
        self.rect(0, SIZE - t, SIZE - 1, SIZE - 1, c)
        self.rect(0, 0, t - 1, SIZE - 1, c)
        self.rect(SIZE - t, 0, SIZE - 1, SIZE - 1, c)

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
        self.rivet(SIZE - 4, 2)
        self.rivet(2, SIZE - 4)
        self.rivet(SIZE - 4, SIZE - 4)


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


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, (name, paint) in enumerate(sorted(TEXTURES.items())):
        t = paint(1000 + i)
        path = os.path.join(OUT, name + '.png')
        with open(path, 'wb') as f:
            f.write(png(t.px))
        print('wrote ' + path)


if __name__ == '__main__':
    main()
