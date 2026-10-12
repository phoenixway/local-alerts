package ua.schaslyve.tryvoha;

/** Converts fresh API observations to one-shot event notifications. */
public final class TransitionPolicy {
    private TransitionPolicy() { }
    public enum Event { NONE, START, END }
    public static Event decide(boolean hasBaseline, boolean previous, boolean current) {
        if (!hasBaseline) return current ? Event.START : Event.NONE;
        if (previous == current) return Event.NONE;
        return current ? Event.START : Event.END;
    }
}
