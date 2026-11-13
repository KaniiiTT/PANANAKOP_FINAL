"""Reference blob-47 autotiler (32x32 tiles) - port to any engine."""
import json
TABLE = json.load(open("blob47_mask_table.json"))["mask_to_cell"]      # mask -> [col,row] in the 8x6 sheet

def blob_mask(solid, x, y):
    h, w = len(solid), len(solid[0])
    def g(xx, yy):
        if 0 <= xx < w and 0 <= yy < h: return bool(solid[yy][xx])
        return yy >= 0                       # outside: solid at sides/bottom, air above the map
    n, e, s, wv = g(x, y-1), g(x+1, y), g(x, y+1), g(x-1, y)
    ne = g(x+1, y-1) and n and e
    se = g(x+1, y+1) and s and e
    sw = g(x-1, y+1) and s and wv
    nw = g(x-1, y-1) and n and wv
    return n | ne << 1 | e << 2 | se << 3 | s << 4 | sw << 5 | wv << 6 | nw << 7

def tile_cell(solid, x, y):
    return TABLE[str(blob_mask(solid, x, y))]          # pixel = (col*32, row*32)

# VARIANTS : pick sheet v0/v1/v2 with ((x*73856093) ^ (y*19349663)) % 3 to hide repetition.
# ANIMATION: animated materials ship one sheet per frame (blocks/<mat>/anim/<mat>_blob47_f<k>.png).
#            Swap the sheet texture every 1/10 s (grass: 8 frames, magma 6, crystal 4, ores 4). Same layout = zero extra logic.
# MERGING   : where two ground families touch (earth/rock/sand/clay/mud/coral/ash) overlay a 2-7px jagged strip of the neighbour's
#             fill tile (mask 255) on the shared edge. See demo scene for the look.
