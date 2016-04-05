
package wigzo.android.sdk;

import android.content.Context;
import android.test.AndroidTestCase;

import java.util.HashMap;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class WigzoTests extends AndroidTestCase {
    Wigzo mUninitedWigzo;
    Wigzo mWigzo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final WigzoStore wigzoStore = new WigzoStore(getContext());
        wigzoStore.clear();

        final WigzoAppStore wigzoAppStore = new WigzoAppStore(getContext());
        wigzoAppStore.clear();

        mUninitedWigzo = new Wigzo();

        mWigzo = new Wigzo();
        mWigzo.init(getContext(), "http://test.wigzo.com", "appkey", "1234");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testConstructor() {
        assertNotNull(mUninitedWigzo.getConnectionQueue());
        assertNull(mUninitedWigzo.getConnectionQueue().getContext());
        assertNull(mUninitedWigzo.getConnectionQueue().getServerURL());
        assertNull(mUninitedWigzo.getConnectionQueue().getAppKey());
        assertNull(mUninitedWigzo.getConnectionQueue().getWigzoStore());
        assertNotNull(mUninitedWigzo.getTimerService());
        assertNull(mUninitedWigzo.getEventQueue());
        assertEquals(0, mUninitedWigzo.getActivityCount());
        assertEquals(0, mUninitedWigzo.getPrevSessionDurationStartTime());
        assertFalse(mUninitedWigzo.getDisableUpdateSessionRequests());
        assertFalse(mUninitedWigzo.isLoggingEnabled());
    }

    public void testSharedInstance() {
        Wigzo sharedWigzo = Wigzo.sharedInstance();
        assertNotNull(sharedWigzo);
        assertSame(sharedWigzo, Wigzo.sharedInstance());
    }

    public void testInitWithNoDeviceID() {
        mUninitedWigzo = spy(mUninitedWigzo);
        mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey", null);
        verify(mUninitedWigzo).init(getContext(), "http://test.wigzo.com", "appkey", null);
    }

    public void testInit_nullContext() {
        try {
            mUninitedWigzo.init(null, "http://test.wigzo.com", "appkey", "1234");
            fail("expected null context to throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            // success!
        }
    }

    public void testInit_nullServerURL() {
        try {
            mUninitedWigzo.init(getContext(), null, "appkey", "1234");
            fail("expected null server URL to throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            // success!
        }
    }

    public void testInit_emptyServerURL() {
        try {
            mUninitedWigzo.init(getContext(), "", "appkey", "1234");
            fail("expected empty server URL to throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            // success!
        }
    }

    public void testInit_invalidServerURL() {
        try {
            mUninitedWigzo.init(getContext(), "not-a-valid-server-url", "appkey", "1234");
            fail("expected invalid server URL to throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            // success!
        }
    }

    public void testInit_nullAppKey() {
        try {
            mUninitedWigzo.init(getContext(), "http://test.wigzo.com", null, "1234");
            fail("expected null app key to throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            // success!
        }
    }

    public void testInit_emptyAppKey() {
        try {
            mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "", "1234");
            fail("expected empty app key to throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            // success!
        }
    }

    public void testInit_nullDeviceID() {
        // null device ID is okay because it tells Wigzo to use OpenUDID
       mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey", null);
    }

    public void testInit_emptyDeviceID() {
        try {
            mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey", "");
            fail("expected empty device ID to throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            // success!
        }
    }

    public void testInit_twiceWithSameParams() {
        final String deviceID = "1234";
        final String appKey = "appkey";
        final String serverURL = "http://test.wigzo.com";

        mUninitedWigzo.init(getContext(), serverURL, appKey, deviceID);
        final EventQueue expectedEventQueue = mUninitedWigzo.getEventQueue();

        final ConnectionQueue expectedConnectionQueue = mUninitedWigzo.getConnectionQueue();
        final WigzoStore expectedWigzoStore = expectedConnectionQueue.getWigzoStore();
        final WigzoAppStore extectedWigzoAppStore = expectedEventQueue.getWigzoAppStore_();
        assertNotNull(expectedEventQueue);
        assertNotNull(expectedConnectionQueue);
        assertNotNull(expectedWigzoStore);
        assertNotNull(extectedWigzoAppStore);

        // second call with same params should succeed, no exception thrown
        mUninitedWigzo.init(getContext(), serverURL, appKey, deviceID);

        assertSame(expectedEventQueue, mUninitedWigzo.getEventQueue());
        assertSame(expectedConnectionQueue, mUninitedWigzo.getConnectionQueue());
        assertSame(expectedWigzoStore, mUninitedWigzo.getConnectionQueue().getWigzoStore());
        assertSame(extectedWigzoAppStore, mUninitedWigzo.getConnectionQueue().getWigzoAppStore());
        assertSame(getContext(), mUninitedWigzo.getConnectionQueue().getContext());
        assertEquals(serverURL, mUninitedWigzo.getConnectionQueue().getServerURL());
        assertEquals(appKey, mUninitedWigzo.getConnectionQueue().getAppKey());
        assertSame(mUninitedWigzo.getConnectionQueue().getWigzoStore(), mUninitedWigzo.getEventQueue().getWigzoStore());
        assertSame(mUninitedWigzo.getConnectionQueue().getWigzoAppStore(), mUninitedWigzo.getEventQueue().getWigzoAppStore_());
    }

    public void testInit_twiceWithDifferentContext() {
        mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey", "1234");
        // changing context is okay since SharedPrefs are global singletons
        mUninitedWigzo.init(mock(Context.class), "http://test.wigzo.com", "appkey", "1234");
    }

    public void testInit_twiceWithDifferentServerURL() {
        mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey", "1234");
        try {
            mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey", "1234");
            fail("expected IllegalStateException to be thrown when calling init a second time with different serverURL");
        }
        catch (IllegalStateException ignored) {
            // success!
        }
    }

    public void testInit_twiceWithDifferentAppKey() {
        mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey1", "1234");
        try {
            mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey2", "1234");
            fail("expected IllegalStateException to be thrown when calling init a second time with different serverURL");
        }
        catch (IllegalStateException ignored) {
            // success!
        }
    }

    public void testInit_twiceWithDifferentDeviceID() {
        mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey", "1234");
        try {
            mUninitedWigzo.init(getContext(), "http://test.wigzo.com", "appkey", "4321");
            fail("expected IllegalStateException to be thrown when calling init a second time with different serverURL");
        }
        catch (IllegalStateException ignored) {
            // success!
        }
    }

    public void testInit_normal() {
        final String deviceID = "1234";
        final String appKey = "appkey";
        final String serverURL = "http://test.wigzo.com";

        mUninitedWigzo.init(getContext(), serverURL, appKey, deviceID);

        assertSame(getContext(), mUninitedWigzo.getConnectionQueue().getContext());
        assertEquals(serverURL, mUninitedWigzo.getConnectionQueue().getServerURL());
        assertEquals(appKey, mUninitedWigzo.getConnectionQueue().getAppKey());
        assertNotNull(mUninitedWigzo.getConnectionQueue().getWigzoStore());
        assertNotNull(mUninitedWigzo.getConnectionQueue().getWigzoAppStore());

        assertNotNull(mUninitedWigzo.getEventQueue());
        assertSame(mUninitedWigzo.getConnectionQueue().getWigzoStore(), mUninitedWigzo.getEventQueue().getWigzoStore());
        assertSame(mUninitedWigzo.getConnectionQueue().getWigzoAppStore(), mUninitedWigzo.getEventQueue().getWigzoAppStore_());
    }

    public void testHalt_notInitialized() {
        mUninitedWigzo.halt();
        assertNotNull(mUninitedWigzo.getConnectionQueue());
        assertNull(mUninitedWigzo.getConnectionQueue().getContext());
        assertNull(mUninitedWigzo.getConnectionQueue().getServerURL());
        assertNull(mUninitedWigzo.getConnectionQueue().getAppKey());
        assertNull(mUninitedWigzo.getConnectionQueue().getWigzoStore());
        assertNull(mUninitedWigzo.getConnectionQueue().getWigzoAppStore());
        assertNotNull(mUninitedWigzo.getTimerService());
        assertNull(mUninitedWigzo.getEventQueue());
        assertEquals(0, mUninitedWigzo.getActivityCount());
        assertEquals(0, mUninitedWigzo.getPrevSessionDurationStartTime());
    }

    public void testHalt() {
        final WigzoStore mockWigzoStore = mock(WigzoStore.class);
        mWigzo.getConnectionQueue().setWigzoStore(mockWigzoStore);
        final WigzoAppStore mockWigzoAppStore = mock(WigzoAppStore.class);
        mWigzo.getConnectionQueue().setWigzoAppStore(mockWigzoAppStore);
        mWigzo.onStart(null);
        assertTrue(0 != mWigzo.getPrevSessionDurationStartTime());
        assertTrue(0 != mWigzo.getActivityCount());
        assertNotNull(mWigzo.getEventQueue());
        assertNotNull(mWigzo.getConnectionQueue().getContext());
        assertNotNull(mWigzo.getConnectionQueue().getServerURL());
        assertNotNull(mWigzo.getConnectionQueue().getAppKey());
        assertNotNull(mWigzo.getConnectionQueue().getContext());

        mWigzo.halt();

        verify(mockWigzoStore).clear();
        verify(mockWigzoAppStore).clear();
        assertNotNull(mWigzo.getConnectionQueue());
        assertNull(mWigzo.getConnectionQueue().getContext());
        assertNull(mWigzo.getConnectionQueue().getServerURL());
        assertNull(mWigzo.getConnectionQueue().getAppKey());
        assertNull(mWigzo.getConnectionQueue().getWigzoStore());
        assertNull(mWigzo.getConnectionQueue().getWigzoAppStore());
        assertNotNull(mWigzo.getTimerService());
        assertNull(mWigzo.getEventQueue());
        assertEquals(0, mWigzo.getActivityCount());
        assertEquals(0, mWigzo.getPrevSessionDurationStartTime());
    }

    public void testOnStart_initNotCalled() {
        try {
            mUninitedWigzo.onStart(null);
            fail("expected calling onStart before init to throw IllegalStateException");
        } catch (IllegalStateException ignored) {
            // success!
        }
    }

    public void testOnStart_firstCall() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        mWigzo.onStart(null);

        assertEquals(1, mWigzo.getActivityCount());
        final long prevSessionDurationStartTime = mWigzo.getPrevSessionDurationStartTime();
        assertTrue(prevSessionDurationStartTime > 0);
        assertTrue(prevSessionDurationStartTime <= System.nanoTime());
        verify(mockConnectionQueue).beginSession();
    }

    public void testOnStart_subsequentCall() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        mWigzo.onStart(null); // first call to onStart
        final long prevSessionDurationStartTime = mWigzo.getPrevSessionDurationStartTime();
        mWigzo.onStart(null); // second call to onStart

        assertEquals(2, mWigzo.getActivityCount());
        assertEquals(prevSessionDurationStartTime, mWigzo.getPrevSessionDurationStartTime());
        verify(mockConnectionQueue).beginSession();
    }

    public void testOnStop_initNotCalled() {
        try {
            mUninitedWigzo.onStop();
            fail("expected calling onStop before init to throw IllegalStateException");
        } catch (IllegalStateException ignored) {
            // success!
        }
    }

    public void testOnStop_unbalanced() {
        try {
            mWigzo.onStop();
            fail("expected calling onStop before init to throw IllegalStateException");
        } catch (IllegalStateException ignored) {
            // success!
        }
    }

    public void testOnStop_reallyStopping_emptyEventQueue() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        mWigzo.onStart(null);
        mWigzo.onStop();

        assertEquals(0, mWigzo.getActivityCount());
        assertEquals(0, mWigzo.getPrevSessionDurationStartTime());
        verify(mockConnectionQueue).endSession(0);
        verify(mockConnectionQueue, times(0)).recordEvents(anyString());
    }

    public void testOnStop_reallyStopping_nonEmptyEventQueue() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        mWigzo.setEventQueue(mockEventQueue);

        when(mockEventQueue.size()).thenReturn(1);
        final String eventStr = "blahblahblahblah";
        when(mockEventQueue.events()).thenReturn(eventStr);

        mWigzo.onStart(null);
        mWigzo.onStop();

        assertEquals(0, mWigzo.getActivityCount());
        assertEquals(0, mWigzo.getPrevSessionDurationStartTime());
        verify(mockConnectionQueue).endSession(0);
        verify(mockConnectionQueue).recordEvents(eventStr);
    }

    public void testOnStop_notStopping() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        mWigzo.onStart(null);
        mWigzo.onStart(null);
        final long prevSessionDurationStartTime = mWigzo.getPrevSessionDurationStartTime();
        mWigzo.onStop();

        assertEquals(1, mWigzo.getActivityCount());
        assertEquals(prevSessionDurationStartTime, mWigzo.getPrevSessionDurationStartTime());
        verify(mockConnectionQueue, times(0)).endSession(anyInt());
        verify(mockConnectionQueue, times(0)).recordEvents(anyString());
    }

    public void testRecordEvent_keyOnly() {
        final String eventKey = "eventKey";
        final Wigzo wigzo = spy(mWigzo);
        doNothing().when(wigzo).recordEvent(eventKey, null, 1, 0.0d);
        wigzo.recordEvent(eventKey);
        verify(wigzo).recordEvent(eventKey, null, 1, 0.0d);
    }

    public void testRecordEvent_keyAndCount() {
        final String eventKey = "eventKey";
        final int count = 42;
        final Wigzo wigzo = spy(mWigzo);
        doNothing().when(wigzo).recordEvent(eventKey, null, count, 0.0d);
        wigzo.recordEvent(eventKey, count);
        verify(wigzo).recordEvent(eventKey, null, count, 0.0d);
    }

    public void testRecordEvent_keyAndCountAndSum() {
        final String eventKey = "eventKey";
        final int count = 42;
        final double sum = 3.0d;
        final Wigzo wigzo = spy(mWigzo);
        doNothing().when(wigzo).recordEvent(eventKey, null, count, sum);
        wigzo.recordEvent(eventKey, count, sum);
        verify(wigzo).recordEvent(eventKey, null, count, sum);
    }

    public void testRecordEvent_keyAndSegmentationAndCount() {
        final String eventKey = "eventKey";
        final int count = 42;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", "segvalue1");
        final Wigzo wigzo = spy(mWigzo);
        doNothing().when(wigzo).recordEvent(eventKey, segmentation, count, 0.0d);
        wigzo.recordEvent(eventKey, segmentation, count);
        verify(wigzo).recordEvent(eventKey, segmentation, count, 0.0d);
    }

    public void testRecordEvent_initNotCalled() {
        final String eventKey = "eventKey";
        final int count = 42;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", "segvalue1");

        try {
            mUninitedWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalStateException when recordEvent called before init");
        } catch (IllegalStateException ignored) {
            // success
        }
    }

    public void testRecordEvent_nullKey() {
        final String eventKey = null;
        final int count = 42;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", "segvalue1");

        try {
            //noinspection ConstantConditions
            mWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalArgumentException when recordEvent called with null key");
        } catch (IllegalArgumentException ignored) {
            // success
        }
    }

    public void testRecordEvent_emptyKey() {
        final String eventKey = "";
        final int count = 42;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", "segvalue1");

        try {
            mWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalArgumentException when recordEvent called with empty key");
        } catch (IllegalArgumentException ignored) {
            // success
        }
    }

    public void testRecordEvent_countIsZero() {
        final String eventKey = "";
        final int count = 0;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", "segvalue1");

        try {
            mWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalArgumentException when recordEvent called with count=0");
        } catch (IllegalArgumentException ignored) {
            // success
        }
    }

    public void testRecordEvent_countIsNegative() {
        final String eventKey = "";
        final int count = -1;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", "segvalue1");

        try {
            mWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalArgumentException when recordEvent called with a negative count");
        } catch (IllegalArgumentException ignored) {
            // success
        }
    }

    public void testRecordEvent_segmentationHasNullKey() {
        final String eventKey = "";
        final int count = 1;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put(null, "segvalue1");

        try {
            mWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalArgumentException when recordEvent called with segmentation with null key");
        } catch (IllegalArgumentException ignored) {
            // success
        }
    }

    public void testRecordEvent_segmentationHasEmptyKey() {
        final String eventKey = "";
        final int count = 1;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("", "segvalue1");

        try {
            mWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalArgumentException when recordEvent called with segmentation with empty key");
        } catch (IllegalArgumentException ignored) {
            // success
        }
    }

    public void testRecordEvent_segmentationHasNullValue() {
        final String eventKey = "";
        final int count = 1;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", null);

        try {
            mWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalArgumentException when recordEvent called with segmentation with null value");
        } catch (IllegalArgumentException ignored) {
            // success
        }
    }

    public void testRecordEvent_segmentationHasEmptyValue() {
        final String eventKey = "";
        final int count = 1;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", "");

        try {
            mWigzo.recordEvent(eventKey, segmentation, count, sum);
            fail("expected IllegalArgumentException when recordEvent called with segmentation with empty value");
        } catch (IllegalArgumentException ignored) {
            // success
        }
    }

    public void testRecordEvent() {
        final String eventKey = "eventKey";
        final int count = 42;
        final double sum = 3.0d;
        final HashMap<String, String> segmentation = new HashMap<String, String>(1);
        segmentation.put("segkey1", "segvalue1");

        final EventQueue mockEventQueue = mock(EventQueue.class);
        mWigzo.setEventQueue(mockEventQueue);

        final Wigzo wigzo = spy(mWigzo);
        doNothing().when(wigzo).sendEventsIfNeeded();
        wigzo.recordEvent(eventKey, segmentation, count, sum);

        verify(mockEventQueue).recordEvent(eventKey, segmentation, count, sum);
        verify(wigzo).sendEventsIfNeeded();
    }

    public void testSendEventsIfNeeded_emptyQueue() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        when(mockEventQueue.size()).thenReturn(0);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.sendEventsIfNeeded();

        verify(mockEventQueue, times(0)).events();
        verifyZeroInteractions(mockConnectionQueue);
    }

    public void testSendEventsIfNeeded_lessThanThreshold() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        when(mockEventQueue.size()).thenReturn(9);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.sendEventsIfNeeded();

        verify(mockEventQueue, times(0)).events();
        verifyZeroInteractions(mockConnectionQueue);
    }

    public void testSendEventsIfNeeded_equalToThreshold() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        when(mockEventQueue.size()).thenReturn(10);
        final String eventData = "blahblahblah";
        when(mockEventQueue.events()).thenReturn(eventData);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.sendEventsIfNeeded();

        verify(mockEventQueue, times(1)).events();
        verify(mockConnectionQueue, times(1)).recordEvents(eventData);
    }

    public void testSendEventsIfNeeded_moreThanThreshold() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        when(mockEventQueue.size()).thenReturn(20);
        final String eventData = "blahblahblah";
        when(mockEventQueue.events()).thenReturn(eventData);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.sendEventsIfNeeded();

        verify(mockEventQueue, times(1)).events();
        verify(mockConnectionQueue, times(1)).recordEvents(eventData);
    }

    public void testOnTimer_noActiveSession() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.onTimer();

        verifyZeroInteractions(mockConnectionQueue, mockEventQueue);
    }

    public void testOnTimer_activeSession_emptyEventQueue() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        when(mockEventQueue.size()).thenReturn(0);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.onStart(null);
        mWigzo.onTimer();

        verify(mockConnectionQueue).updateSession(0);
        verify(mockConnectionQueue, times(0)).recordEvents(anyString());
    }

    public void testOnTimer_activeSession_nonEmptyEventQueue() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        when(mockEventQueue.size()).thenReturn(1);
        final String eventData = "blahblahblah";
        when(mockEventQueue.events()).thenReturn(eventData);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.onStart(null);
        mWigzo.onTimer();

        verify(mockConnectionQueue).updateSession(0);
        verify(mockConnectionQueue).recordEvents(eventData);
    }

    public void testOnTimer_activeSession_emptyEventQueue_sessionTimeUpdatesDisabled() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);
        mWigzo.setDisableUpdateSessionRequests(true);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        when(mockEventQueue.size()).thenReturn(0);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.onStart(null);
        mWigzo.onTimer();

        verify(mockConnectionQueue, times(0)).updateSession(anyInt());
        verify(mockConnectionQueue, times(0)).recordEvents(anyString());
    }

    public void testOnTimer_activeSession_nonEmptyEventQueue_sessionTimeUpdatesDisabled() {
        final ConnectionQueue mockConnectionQueue = mock(ConnectionQueue.class);
        mWigzo.setConnectionQueue(mockConnectionQueue);
        mWigzo.setDisableUpdateSessionRequests(true);

        final EventQueue mockEventQueue = mock(EventQueue.class);
        when(mockEventQueue.size()).thenReturn(1);
        final String eventData = "blahblahblah";
        when(mockEventQueue.events()).thenReturn(eventData);
        mWigzo.setEventQueue(mockEventQueue);

        mWigzo.onStart(null);
        mWigzo.onTimer();

        verify(mockConnectionQueue, times(0)).updateSession(anyInt());
        verify(mockConnectionQueue).recordEvents(eventData);
    }

    public void testRoundedSecondsSinceLastSessionDurationUpdate() {
        long prevSessionDurationStartTime = System.nanoTime() - 1000000000;
        mWigzo.setPrevSessionDurationStartTime(prevSessionDurationStartTime);
        assertEquals(1, mWigzo.roundedSecondsSinceLastSessionDurationUpdate());

        prevSessionDurationStartTime = System.nanoTime() - 2000000000;
        mWigzo.setPrevSessionDurationStartTime(prevSessionDurationStartTime);
        assertEquals(2, mWigzo.roundedSecondsSinceLastSessionDurationUpdate());

        prevSessionDurationStartTime = System.nanoTime() - 1600000000;
        mWigzo.setPrevSessionDurationStartTime(prevSessionDurationStartTime);
        assertEquals(2, mWigzo.roundedSecondsSinceLastSessionDurationUpdate());

        prevSessionDurationStartTime = System.nanoTime() - 1200000000;
        mWigzo.setPrevSessionDurationStartTime(prevSessionDurationStartTime);
        assertEquals(1, mWigzo.roundedSecondsSinceLastSessionDurationUpdate());
    }

    public void testIsValidURL_badURLs() {
        assertFalse(Wigzo.isValidURL(null));
        assertFalse(Wigzo.isValidURL(""));
        assertFalse(Wigzo.isValidURL(" "));
        assertFalse(Wigzo.isValidURL("blahblahblah.com"));
    }

    public void testIsValidURL_goodURL() {
        assertTrue(Wigzo.isValidURL("http://test.wigzo.com"));
    }

    public void testCurrentTimestamp() {
        final int testTimestamp = (int) (System.currentTimeMillis() / 1000l);
        final int actualTimestamp = Wigzo.currentTimestamp();
        assertTrue(((testTimestamp - 1) <= actualTimestamp) && ((testTimestamp + 1) >= actualTimestamp));
    }

    public void testSetDisableUpdateSessionRequests() {
        assertFalse(mWigzo.getDisableUpdateSessionRequests());
        mWigzo.setDisableUpdateSessionRequests(true);
        assertTrue(mWigzo.getDisableUpdateSessionRequests());
        mWigzo.setDisableUpdateSessionRequests(false);
        assertFalse(mWigzo.getDisableUpdateSessionRequests());
    }

    public void testLoggingFlag() {
        assertFalse(mUninitedWigzo.isLoggingEnabled());
        mUninitedWigzo.setLoggingEnabled(true);
        assertTrue(mUninitedWigzo.isLoggingEnabled());
        mUninitedWigzo.setLoggingEnabled(false);
        assertFalse(mUninitedWigzo.isLoggingEnabled());
    }
}
