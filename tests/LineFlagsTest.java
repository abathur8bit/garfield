import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineFlagsTest {

    @Test
    void setOne() {
        LineFlags lineFlags = new LineFlags();
        lineFlags.set(1,1);
        int flag = lineFlags.get(1);
        assertEquals(1,flag);
    }

    @Test
    void setTwo() {
        LineFlags lf = new LineFlags();
        lf.set(1,1);
        lf.set(1,2);
        assertEquals(3,lf.get(1));
    }

    @Test
    void setMask() {
        LineFlags lf = new LineFlags();
        lf.set(1,5);    //mask 0101
        assertTrue (lf.isSet(1,1)); //0001
        assertFalse(lf.isSet(1,2)); //0010
        assertFalse(lf.isSet(1,3)); //0011
        assertTrue (lf.isSet(1,4)); //0100
    }

    @Test
    void isSet() {
        LineFlags lf = new LineFlags();
        lf.set(0,1);
        assertTrue(lf.isSet(0,1));
    }

    @Test
    void isSetEmpty() {
        LineFlags lf = new LineFlags();
        assertFalse (lf.isSet(0,1));
        assertFalse (lf.isSet(1,1));
        assertEquals(0,lf.size());
        lf.get(1);
        assertEquals(0,lf.size());
    }

    @Test
    void count() {
        LineFlags lf = new LineFlags();
        assertEquals(0,lf.size());
        lf.set(1,1);
        assertEquals(1,lf.size());
        lf.set(1,2);
        assertEquals(1,lf.size());
        lf.set(2,4);
        assertEquals(2,lf.size());
    }

    @Test
    void clear() {
        LineFlags lf = new LineFlags();
        lf.set(1,1);
        assertEquals(1,lf.size());
        lf.clear();
        assertEquals(0,lf.size());
    }

    @Test
    void reset() {
        LineFlags lf = new LineFlags();
        lf.set(1,5);                //0101 flag value
        assertEquals(5,lf.get(1));

        assertTrue(lf.isSet(1,1));  //0001 this flag should be set
        assertTrue(lf.isSet(1,4));  //0100 this flag should be set

        lf.reset(1,1);              //0100 new flag value
        assertEquals(4,lf.get(1));
        assertTrue(lf.isSet(1,4));  //0100 this flag should be set
        assertFalse(lf.isSet(1,1)); //0001 this flag should NOT be set
    }

    /** Check reset(int mask) will reset all of the specified bits. */
    @Test
    void resetAll() {
        LineFlags lf = new LineFlags();

        //set
        lf.set(1,3);    //0011
        lf.set(2,12);   //1100
        assertEquals(3,lf.get(1));
        assertEquals(12,lf.get(2));

        //reset and verify
        lf.reset(6); //0110
        assertEquals(1,lf.get(1));
        assertEquals(8,lf.get(2));
    }

    @Test
    void getEmpty() {
        LineFlags lf = new LineFlags();
        assertEquals(0,lf.get(99)); //nothing exists
    }
}