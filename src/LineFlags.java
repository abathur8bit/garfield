import java.util.HashMap;

public class LineFlags {
    private HashMap<Integer,Integer> lineFlags = new HashMap<>();

    public int get(int lineNum) {
        return lineFlags.getOrDefault(lineNum,0);
    }

    public void set(int lineNum,int mask) {
        int lineFlag = get(lineNum);
        lineFlag |= mask;
        lineFlags.put(lineNum,lineFlag);
    }

    public void reset(int lineNum,int mask) {
        if(lineFlags.containsKey(lineNum)) {
            int lineFlag = get(lineNum) & 0xFFFFFFFF-mask;
            lineFlags.put(lineNum,lineFlag);
        }
    }

    public boolean isSet(int lineNum,int mask) {
        int lineFlag = get(lineNum);
        return (lineFlag&mask) == mask;
    }

    public int size() {
        return lineFlags.size();
    }

    public void clear() {
        lineFlags.clear();
    }
}
