"""Writes the GameTest structure templates (gzipped NBT). Run from the repo root: python3 tools/gen_structures.py"""
import gzip
import os
import struct

DATA_VERSION = 3955  # Minecraft 1.21.1
OUT = os.path.join("src", "main", "resources", "data", "curtaindoors", "structure")

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def name(n):
    b = n.encode()
    return struct.pack(">H", len(b)) + b


def payload(tag, value):
    if tag == TAG_INT:
        return struct.pack(">i", value)
    if tag == TAG_STRING:
        return name(value)
    if tag == TAG_LIST:
        elem_tag, items = value
        out = struct.pack(">bi", elem_tag if items else elem_tag, len(items))
        return out + b"".join(payload(elem_tag, i) for i in items)
    if tag == TAG_COMPOUND:
        out = b""
        for key, (t, v) in value.items():
            out += struct.pack(">b", t) + name(key) + payload(t, v)
        return out + struct.pack(">b", TAG_END)
    raise ValueError(tag)


def structure(size, blocks, palette):
    root = {
        "DataVersion": (TAG_INT, DATA_VERSION),
        "size": (TAG_LIST, (TAG_INT, list(size))),
        "palette": (TAG_LIST, (TAG_COMPOUND, [{"Name": (TAG_STRING, p)} for p in palette])),
        "blocks": (TAG_LIST, (TAG_COMPOUND, [
            {"pos": (TAG_LIST, (TAG_INT, list(pos))), "state": (TAG_INT, state)} for pos, state in blocks
        ])),
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
    }
    return struct.pack(">b", TAG_COMPOUND) + name("") + payload(TAG_COMPOUND, root)


def platform():
    """5x6x5: a stone floor at y=0, air above. (In a test, structure y=0 is relative y=1.)"""
    size = (5, 6, 5)
    blocks = []
    for x in range(5):
        for z in range(5):
            blocks.append(((x, 0, z), 0))
            for y in range(1, 6):
                blocks.append(((x, y, z), 1))
    return structure(size, blocks, ["minecraft:stone", "minecraft:air"])


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, "platform.nbt")
    with gzip.open(path, "wb") as f:
        f.write(platform())
    print("wrote", path)
