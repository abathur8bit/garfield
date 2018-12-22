import java.util.HashMap;

/**
 * Keeps a list of bit flags. You pass in a line number, and bit mask, and LineFlag will
 * take care of setting/resetting the bits and adding to the hashmap.
 * Each flag is an integer that can hold 0xFFFFFFFF flags.
 * Get will return 0 if the flag doesn't exist, and a new flag is NOT created.
 */
public class LineFlags {
    private HashMap<Integer,Integer> lineFlags = new HashMap<>();

    /** Return the current flag value for the given line number. */
    public int get(int lineNum) {
        return lineFlags.getOrDefault(lineNum,0);
    }

    /**
     * Sets the bit mask on the specified line. If the flag didn't already exist, it is created.
     * If line did have a flag, then the new bits are or'ed with it. If you pass in a value of 3, you are
     * setting bits "11". If you pass in 5 you are setting "101".
     *
     * @param lineNum The line you are interested in.
     * @param mask The bits you are wanting to set.
     */
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

    /**
     * Checks if the bits in mask are set.
     *
     * @param lineNum The line you are interested in.
     * @param mask bits to ensure are set.
     * @return true if all the bits in mask are set, false otherwise.
     */
    public boolean isSet(int lineNum,int mask) {
        int lineFlag = get(lineNum);
        return (lineFlag&mask) == mask;
    }

    /** The number of flags. */
    public int size() {
        return lineFlags.size();
    }

    /** Remove all flags. */
    public void clear() {
        lineFlags.clear();
    }
}
