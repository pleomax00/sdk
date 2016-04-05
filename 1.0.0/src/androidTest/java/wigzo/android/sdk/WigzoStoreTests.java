
package wigzo.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WigzoStoreTests extends AndroidTestCase {
    WigzoStore store;
    WigzoAppStore mStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        store = new WigzoStore(getContext());
        store.clear();

        mStore = new WigzoAppStore(getContext());
        mStore.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        store.clear();
        mStore.clear();
        super.tearDown();
    }

    public void testConstructor_nullContext() {
        try {
            new WigzoStore(null);
            new WigzoAppStore(null);
            fail("expected IllegalArgumentException when calling WigzoStore() ctor with null context");
        } catch (IllegalArgumentException ignored) {
            // success!
        }
    }

    public void testConstructor() {
        Context mockContext = mock(Context.class);
        new WigzoStore(mockContext);
        verify(mockContext).getSharedPreferences("WIGZO_STORE", Context.MODE_PRIVATE);
        new WigzoAppStore(mockContext);
        verify(mockContext).getSharedPreferences("MOBILE_STORE", Context.MODE_PRIVATE);
    }

    public void testConnections_prefIsNull() {
        // the clear() call in setUp ensures the pref is not present
        assertTrue(Arrays.equals(new String[0], store.connections()));
        assertTrue(Arrays.equals(new String[0], mStore.connections()));
    }

    public void testConnections_prefIsEmptyString() {
        // the following two calls will result in the pref being an empty string
        final String connStr = "blah";
        store.addConnection(connStr);
        store.removeConnection(connStr);
        assertTrue(Arrays.equals(new String[0], store.connections()));

        mStore.addConnection(connStr);
        mStore.removeConnection(connStr);
        assertTrue(Arrays.equals(new String[0], mStore.connections()));
    }

    public void testConnections_prefHasSingleValue() {
        final String connStr = "blah";
        store.addConnection(connStr);
        assertTrue(Arrays.equals(new String[]{connStr}, store.connections()));

        mStore.addConnection(connStr);
        assertTrue(Arrays.equals(new String[]{connStr}, mStore.connections()));
    }

    public void testConnections_prefHasTwoValues() {
        final String connStr1 = "blah1";
        final String connStr2 = "blah2";
        store.addConnection(connStr1);
        store.addConnection(connStr2);
        assertTrue(Arrays.equals(new String[]{connStr1,connStr2}, store.connections()));

        mStore.addConnection(connStr1);
        mStore.addConnection(connStr2);
        assertTrue(Arrays.equals(new String[]{connStr1,connStr2}, mStore.connections()));
    }

    public void testEvents_prefIsNull() {
        // the clear() call in setUp ensures the pref is not present
        assertTrue(Arrays.equals(new String[0], store.events()));
        assertTrue(Arrays.equals(new String[0], mStore.events()));
    }

    public void testEvents_prefIsEmptyString() {
        // the following two calls will result in the pref being an empty string
        store.addEvent("eventKey", null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        store.removeEvents(store.eventsList());
        assertTrue(Arrays.equals(new String[0], store.events()));

        mStore.addEvent("eventKey", null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        mStore.removeEvents(mStore.eventsList());
        assertTrue(Arrays.equals(new String[0], mStore.events()));
    }

    public void testEvents_prefHasSingleValue() throws JSONException {
        final String eventKey = "eventKey";
        store.addEvent(eventKey, null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        final String[] eventJSONStrs = store.events();
        final JSONObject eventJSONObj = new JSONObject(eventJSONStrs[0]);
        assertEquals(eventKey, eventJSONObj.getString("key"));

        mStore.addEvent(eventKey, null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        final String[] eventJSONStrs1 = mStore.events();
        final JSONObject eventJSONObj1 = new JSONObject(eventJSONStrs1[0]);
        assertEquals(eventKey, eventJSONObj1.getString("key"));
        // this is good enough, we verify the entire JSON content is written in later unit tests
    }

    public void testEvents_prefHasTwoValues() throws JSONException {
        final String eventKey1 = "eventKey1";
        final String eventKey2 = "eventKey2";
        store.addEvent(eventKey1, null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        store.addEvent(eventKey2, null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        final String[] eventJSONStrs = store.events();
        final JSONObject eventJSONObj1 = new JSONObject(eventJSONStrs[0]);
        assertEquals(eventKey1, eventJSONObj1.getString("key"));
        final JSONObject eventJSONObj2 = new JSONObject(eventJSONStrs[1]);
        assertEquals(eventKey2, eventJSONObj2.getString("key"));

        mStore.addEvent(eventKey1, null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        mStore.addEvent(eventKey2, null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        final String[] eventJSONStrs1 = mStore.events();
        final JSONObject eventJSONObj = new JSONObject(eventJSONStrs1[0]);
        assertEquals(eventKey1, eventJSONObj.getString("key"));
        final JSONObject eventJSONObj3 = new JSONObject(eventJSONStrs1[1]);
        assertEquals(eventKey2, eventJSONObj3.getString("key"));
        // this is good enough, we verify the entire JSON content is written in later unit tests
    }

    public void testEventsList_noEvents() {
        assertEquals(new ArrayList<Event>(0), store.eventsList());
        assertEquals(new ArrayList<Event>(0), mStore.eventsList());
    }

    public void testEventsList_singleEvent() {
        final Event event1 = new Event();
        event1.key = "eventKey1";
        event1.timestamp = Wigzo.currentTimestamp();
        event1.count = 1;
        store.addEvent(event1.key, event1.segmentation, event1.timestamp, event1.hour, event1.dow, event1.count, event1.sum);
        mStore.addEvent(event1.key, event1.segmentation, event1.timestamp, event1.hour, event1.dow, event1.count, event1.sum);
        final List<Event> expected = new ArrayList<Event>(1);
        expected.add(event1);
        final List<Event> actual = store.eventsList();
        assertEquals(expected, actual);
        final List<Event> actuals = mStore.eventsList();
        assertEquals(expected, actuals);
    }

    public void testEventsList_sortingOfMultipleEvents() {
        final Event event1 = new Event();
        event1.key = "eventKey1";
        event1.timestamp = Wigzo.currentTimestamp();
        event1.count = 1;
        final Event event2 = new Event();
        event2.key = "eventKey2";
        event2.timestamp = Wigzo.currentTimestamp() - 60;
        event2.count = 1;
        final Event event3 = new Event();
        event3.key = "eventKey3";
        event3.timestamp = Wigzo.currentTimestamp() - 30;
        event3.count = 1;
        store.addEvent(event1.key, event1.segmentation, event1.timestamp, event1.hour, event1.dow, event1.count, event1.sum);
        store.addEvent(event2.key, event2.segmentation, event2.timestamp, event2.hour, event2.dow, event2.count, event2.sum);
        store.addEvent(event3.key, event3.segmentation, event3.timestamp, event3.hour, event3.dow, event3.count, event3.sum);

        mStore.addEvent(event1.key, event1.segmentation, event1.timestamp, event1.hour, event1.dow, event1.count, event1.sum);
        mStore.addEvent(event2.key, event2.segmentation, event2.timestamp, event2.hour, event2.dow, event2.count, event2.sum);
        mStore.addEvent(event3.key, event3.segmentation, event3.timestamp, event3.hour, event3.dow, event3.count, event3.sum);
        final List<Event> expected = new ArrayList<Event>(3);
        expected.add(event2);
        expected.add(event3);
        expected.add(event1);
        final List<Event> actual = store.eventsList();
        assertEquals(expected, actual);

        final List<Event> actuals = mStore.eventsList();
        assertEquals(expected, actuals);
    }

    public void testEventsList_badJSON() {
        final Event event1 = new Event();
        event1.key = "eventKey1";
        event1.timestamp = Wigzo.currentTimestamp() - 60;
        event1.hour = Wigzo.currentHour();
        event1.dow = Wigzo.currentDayOfWeek();
        event1.count = 1;
        final Event event2 = new Event();
        event2.key = "eventKey2";
        event2.timestamp = Wigzo.currentTimestamp();
        event2.hour = Wigzo.currentHour();
        event2.dow = Wigzo.currentDayOfWeek();
        event2.count = 1;

        final String joinedEventsWithBadJSON = event1.toJSON().toString() + ":::blah:::" + event2.toJSON().toString();
        final SharedPreferences prefs = getContext().getSharedPreferences("WIGZO_STORE", Context.MODE_PRIVATE);
        prefs.edit().putString("EVENTS", joinedEventsWithBadJSON).commit();

        final SharedPreferences prefsmobile = getContext().getSharedPreferences("MOBILE_STORE", Context.MODE_PRIVATE);
        prefsmobile.edit().putString("EVENTS", joinedEventsWithBadJSON).commit();

        final List<Event> expected = new ArrayList<Event>(2);
        expected.add(event1);
        expected.add(event2);
        final List<Event> actual = store.eventsList();
        assertEquals(expected, actual);

        final List<Event> actuals = mStore.eventsList();
        assertEquals(expected, actuals);
    }

    public void testEventsList_EventFromJSONReturnsNull() {
        final Event event1 = new Event();
        event1.key = "eventKey1";
        event1.timestamp = Wigzo.currentTimestamp() - 60;
        event1.hour = Wigzo.currentHour();
        event1.dow = Wigzo.currentDayOfWeek();
        event1.count = 1;
        final Event event2 = new Event();
        event2.key = "eventKey2";
        event2.timestamp = Wigzo.currentTimestamp();
        event2.hour = Wigzo.currentHour();
        event2.dow = Wigzo.currentDayOfWeek();
        event2.count = 1;

        final String joinedEventsWithBadJSON = event1.toJSON().toString() + ":::{\"key\":null}:::" + event2.toJSON().toString();
        final SharedPreferences prefs = getContext().getSharedPreferences("WIGZO_STORE", Context.MODE_PRIVATE);
        prefs.edit().putString("EVENTS", joinedEventsWithBadJSON).commit();

        final SharedPreferences prefsmobile = getContext().getSharedPreferences("MOBILE_STORE", Context.MODE_PRIVATE);
        prefsmobile.edit().putString("EVENTS", joinedEventsWithBadJSON).commit();

        final List<Event> expected = new ArrayList<Event>(2);
        expected.add(event1);
        expected.add(event2);
        final List<Event> actual = store.eventsList();
        assertEquals(expected, actual);

        final List<Event> actuals = mStore.eventsList();
        assertEquals(expected, actuals);
    }

    public void testIsEmptyConnections_prefIsNull() {
        // the clear() call in setUp ensures the pref is not present
        assertTrue(store.isEmptyConnections());
        assertTrue(mStore.isEmptyConnections());
    }

    public void testIsEmptyConnections_prefIsEmpty() {
        // the following two calls will result in the pref being an empty string
        final String connStr = "blah";
        store.addConnection(connStr);
        store.removeConnection(connStr);
        assertTrue(store.isEmptyConnections());

        mStore.addConnection(connStr);
        mStore.removeConnection(connStr);
        assertTrue(mStore.isEmptyConnections());
    }

    public void testIsEmptyConnections_prefIsPopulated() {
        final String connStr = "blah";
        store.addConnection(connStr);
        assertFalse(store.isEmptyConnections());

        mStore.addConnection(connStr);
        assertFalse(mStore.isEmptyConnections());
    }

    public void testAddConnection_nullStr() {
        store.addConnection(null);
        assertTrue(store.isEmptyConnections());

        mStore.addConnection(null);
        assertTrue(mStore.isEmptyConnections());
    }

    public void testAddConnection_emptyStr() {
        store.addConnection("");
        assertTrue(store.isEmptyConnections());
        mStore.addConnection("");
        assertTrue(mStore.isEmptyConnections());
    }

    public void testRemoveConnection_nullStr() {
        store.addConnection("blah");
        store.removeConnection(null);
        assertFalse(store.isEmptyConnections());
        mStore.addConnection("blah");
        mStore.removeConnection(null);
        assertFalse(mStore.isEmptyConnections());
    }

    public void testRemoveConnection_emptyStr() {
        store.addConnection("blah");
        store.removeConnection("");
        assertFalse(store.isEmptyConnections());
        mStore.addConnection("blah");
        mStore.removeConnection("");
        assertFalse(mStore.isEmptyConnections());
    }

    public void testRemoveConnection_firstConn() {
        store.addConnection("blah");
        assertFalse(store.isEmptyConnections());
        store.removeConnection("blah");
        assertTrue(store.isEmptyConnections());

        mStore.addConnection("blah");
        assertFalse(mStore.isEmptyConnections());
        mStore.removeConnection("blah");
        assertTrue(mStore.isEmptyConnections());
    }

    public void testRemoveConnection_notFirstConn() {
        store.addConnection("blah1");
        store.addConnection("blah2");
        assertEquals(2, store.connections().length);
        store.removeConnection("blah2");
        assertEquals(1, store.connections().length);
        mStore.addConnection("blah1");
        mStore.addConnection("blah2");
        assertEquals(2, mStore.connections().length);
        mStore.removeConnection("blah2");
        assertEquals(1, mStore.connections().length);
    }

    public void testRemoveConnection_onlyRemovesFirstMatchingOne() {
        store.addConnection("blah1");
        store.addConnection("blah2");
        store.addConnection("blah1");
        assertEquals(3, store.connections().length);
        store.removeConnection("blah1");
        assertTrue(Arrays.equals(new String[]{"blah2", "blah1"}, store.connections()));
        mStore.addConnection("blah1");
        mStore.addConnection("blah2");
        mStore.addConnection("blah1");
        assertEquals(3, mStore.connections().length);
        mStore.removeConnection("blah1");
        assertTrue(Arrays.equals(new String[]{"blah2", "blah1"}, mStore.connections()));
    }

    public void testAddEvent() {
        final Event event1 = new Event();
        event1.key = "eventKey1";
        event1.timestamp = Wigzo.currentTimestamp() - 60;
        event1.count = 42;
        event1.sum = 3.2;
        event1.segmentation = new HashMap<String, String>(2);
        event1.segmentation.put("segKey1", "segValue1");
        event1.segmentation.put("segKey2", "segValue2");

        store.addEvent(event1.key, event1.segmentation, event1.timestamp, event1.hour, event1.dow, event1.count, event1.sum);
        mStore.addEvent(event1.key, event1.segmentation, event1.timestamp, event1.hour, event1.dow, event1.count, event1.sum);

        final List<Event> addedEvents = store.eventsList();
        assertEquals(1, addedEvents.size());
        final Event addedEvent = addedEvents.get(0);
        assertEquals(event1, addedEvent);
        assertEquals(event1.count, addedEvent.count);
        assertEquals(event1.sum, addedEvent.sum);

        final List<Event> addedEventmobile = mStore.eventsList();
        assertEquals(1, addedEvents.size());
        final Event addedEventmobiles = addedEventmobile.get(0);
        assertEquals(event1, addedEventmobiles);
        assertEquals(event1.count, addedEventmobiles.count);
        assertEquals(event1.sum, addedEventmobiles.sum);
    }

    public void testRemoveEvents() {
        final Event event1 = new Event();
        event1.key = "eventKey1";
        event1.timestamp = Wigzo.currentTimestamp() - 60;
        event1.count = 1;
        final Event event2 = new Event();
        event2.key = "eventKey2";
        event2.timestamp = Wigzo.currentTimestamp() - 30;
        event2.count = 1;
        final Event event3 = new Event();
        event3.key = "eventKey2";
        event3.timestamp = Wigzo.currentTimestamp();
        event3.count = 1;

        store.addEvent(event1.key, event1.segmentation, event1.timestamp, event1.hour, event1.dow, event1.count, event1.sum);
        store.addEvent(event2.key, event2.segmentation, event2.timestamp, event2.hour, event2.dow, event2.count, event2.sum);

        final List<Event> eventsToRemove = store.eventsList();

        store.addEvent(event3.key, event3.segmentation, event3.timestamp, event3.hour, event3.dow, event3.count, event3.sum);

        store.removeEvents(eventsToRemove);

        final List<Event> events = store.eventsList();
        assertEquals(1, events.size());
        assertEquals(event3, events.get(0));

        mStore.addEvent(event1.key, event1.segmentation, event1.timestamp, event1.hour, event1.dow, event1.count, event1.sum);
        mStore.addEvent(event2.key, event2.segmentation, event2.timestamp, event2.hour, event2.dow, event2.count, event2.sum);

        final List<Event> eventsToRemoves = mStore.eventsList();

        mStore.addEvent(event3.key, event3.segmentation, event3.timestamp, event3.hour, event3.dow, event3.count, event3.sum);

        mStore.removeEvents(eventsToRemoves);

        final List<Event> event = mStore.eventsList();
        assertEquals(1, event.size());
        assertEquals(event3, event.get(0));
    }

    public void testClear() {
        final SharedPreferences prefs = getContext().getSharedPreferences("WIGZO_STORE", Context.MODE_PRIVATE);
        assertFalse(prefs.contains("EVENTS"));
        assertFalse(prefs.contains("CONNECTIONS"));
        store.addConnection("blah");
        store.addEvent("eventKey", null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);

        mStore.addConnection("blah");
        mStore.addEvent("eventKey", null, Wigzo.currentTimestamp(), Wigzo.currentHour(), Wigzo.currentDayOfWeek(), 1, 0.0d);
        assertTrue(prefs.contains("EVENTS"));
        assertTrue(prefs.contains("CONNECTIONS"));
        mStore.clear();

        assertFalse(prefs.contains("EVENTS"));
        assertFalse(prefs.contains("CONNECTIONS"));
    }
}
