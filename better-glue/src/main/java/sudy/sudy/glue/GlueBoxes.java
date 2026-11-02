    public static final int MAX_BOXES = 96;

    /**
     * 方块集合 → 盒子列表（已做邻居扩张）。
     * 盒子数超过 MAX_BOXES 时返回 null，由调用方提示，不再静默退回整体包围盒。
     */
    public static List<Box> forCells(Set<BlockPos> cells) {
        List<Box> boxes = decompose(cells);
        if (boxes == null) {
            return null;
        }
        List<Box> result = new ArrayList<>(boxes.size());
        for (Box box : boxes) {
            result.add(expandIntoNeighbors(box, cells));
        }
        return result;
    }
