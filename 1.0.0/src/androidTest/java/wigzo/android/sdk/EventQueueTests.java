
package wigzo.android.sdk;

import android.test.AndroidTestCase;

import org.mockito.ArgumentCaptor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventQueueTests extends AndroidTestCase {
    EventQueue mEventQueue;
    EventQueue mobileEventQueue;
    WigzoStore mMockWigzoStore;
    WigzoAppStore wigzoAppStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mMockWigzoStore = mock(WigzoStore.class);
        mEventQueue = new EventQueue(mMockWigzoStore);

        mobileEventQueue = new EventQueue(wigzoAppStore);
        mEventQueue = new EventQueue(wigzoAppStore);
    }

    public void testConstructor() {
        assertSame(mMockWigzoStore, mEventQueue.getWigzoStore());
        assertSame(wigzoAppStore,mobileEventQueue.getWigzoAppStore_() );

    }

    public void testRecordEvent() {
        final String eventKey = "eventKey";
        final int count = 42;
        final double sum = 3.0d;
        final Map<String, String> segmentation = new HashMap<String, String>(1);
        final int timestamp = Wigzo.currentTimestamp();
        final int hour = Wigzo.currentHour();
        final int dow = Wigzo.currentDayOfWeek();
        final ArgumentCaptor<Integer> arg = ArgumentCaptor.forClass(Integer.class);

        mEventQueue.recordEvent(eventKey, segmentation, count, sum);
        mobileEventQueue.recordEvent(eventKey , segmentation, count, sum);
        verify(mMockWigzoStore).addEvent(eq(eventKey), eq(segmentation), arg.capture(), eq(hour), eq(dow), eq(count), eq(sum));
        verify(wigzoAppStore).addEvent(eq(eventKey),eq(segmentation), arg.capture(), eq(hour), eq(dow), eq(count), eq(sum));
        assertTrue(((timestamp - 1) <= arg.getValue()) && ((timestamp + 1) >= arg.getValue()));
    }

    public void testSize_zeroLenArray() {
        when(mMockWigzoStore.events()).thenReturn(new String[0]);
        when(wigzoAppStore.events()).thenReturn(new String[0]);
        assertEquals(0, mEventQueue.size());
        assertEquals(0, mobileEventQueue.mobilesize());
    }

    public void testSize() {
        when(mMockWigzoStore.events()).thenReturn(new String[2]);
        assertEquals(2, mEventQueue.size());
        when(wigzoAppStore.events()).thenReturn(new String[2]);
        assertEquals(2, mobileEventQueue.mobilesize());
    }

    public void testEvents_emptyList() throws UnsupportedEncodingException {
        final List<Event> eventsList = new ArrayList<Event>();
        when(mMockWigzoStore.eventsList()).thenReturn(eventsList);

        final String expected = URLEncoder.encode("[]", "UTF-8");
        assertEquals(expected, mEventQueue.events());
        verify(mMockWigzoStore).eventsList();

        when(wigzoAppStore.eventsList()).thenReturn(eventsList);

        final String expecte = URLEncoder.encode("[]", "UTF-8");
        assertEquals(expecte, mobileEventQueue.mobileEvents());
        verify(wigzoAppStore).eventsList();
        verify(wigzoAppStore).removeEvents(eventsList);

        verify(wigzoAppStore).removeEvents(eventsList);
    }

    public void testEvents_nonEmptyList() throws UnsupportedEncodingException {
        final List<Event> eventsList = new ArrayList<Event>();
        final Event event1 = new Event();
        event1.key = "event1Key";
        eventsList.add(event1);
        final Event event2 = new Event();
        event2.key = "event2Key";
        eventsList.add(event2);
        when(mMockWigzoStore.eventsList()).thenReturn(eventsList);

        final String jsonToEncode = "[" + event1.toJSON().toString() + "," + event2.toJSON().toString() + "]";
        final String expected = URLEncoder.encode(jsonToEncode, "UTF-8");
        assertEquals(expected, mEventQueue.events());
        verify(wigzoAppStore).eventsList();

        when(wigzoAppStore.eventsList()).thenReturn(eventsList);

        final String jsonToEncode1 = "[" + event1.toJSON().toString() + "," + event2.toJSON().toString() + "]";
        final String expected1 = URLEncoder.encode(jsonToEncode1, "UTF-8");
        assertEquals(expected1, mobileEventQueue.mobileEvents());
        verify(wigzoAppStore).eventsList();

        verify(wigzoAppStore).removeEvents(eventsList);
        verify(mMockWigzoStore).removeEvents(eventsList);
    }
}
