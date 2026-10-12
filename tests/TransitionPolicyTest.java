import ua.schaslyve.tryvoha.TransitionPolicy;
import static ua.schaslyve.tryvoha.TransitionPolicy.Event.*;

public final class TransitionPolicyTest {
    static int n=0;
    static void check(String name, TransitionPolicy.Event expected, boolean had, boolean was, boolean now) {
        TransitionPolicy.Event actual=TransitionPolicy.decide(had,was,now);
        if (actual != expected) throw new AssertionError(name+": expected "+expected+" got "+actual);
        System.out.println("PASS: "+name); n++;
    }
    public static void main(String[] args) {
        check("first clear must not notify", NONE, false, false, false);
        check("first active must alert", START, false, false, true);
        check("repeated active must not duplicate", NONE, true, true, true);
        check("active to clear produces one all-clear", END, true, true, false);
        check("repeated clear must not duplicate", NONE, true, false, false);
        check("clear to active alerts", START, true, false, true);
        System.out.println(n+"/"+n+" transition tests passed");
    }
}
