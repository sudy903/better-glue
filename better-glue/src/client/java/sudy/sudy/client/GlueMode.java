package sudy.sudy.client;

public enum GlueMode {
    NORMAL("正常模式"),
    QUICK_SELECT("快速框选"),
    FINE_REMOVE("精细消除");

    private final String label;

    GlueMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public GlueMode next() {
        GlueMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public GlueMode previous() {
        GlueMode[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }
}
