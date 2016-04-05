
package wigzo.android.sdk;

import android.test.AndroidTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConnectionProcessorTests extends AndroidTestCase {
    ConnectionProcessor connectionProcessor;
    WigzoStore mockStore;
    WigzoAppStore wigzoAppStore;
    DeviceId mockDeviceId;
    String testDeviceId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mockStore = mock(WigzoStore.class);
        wigzoAppStore = mock(WigzoAppStore.class);
        mockDeviceId = mock(DeviceId.class);
        connectionProcessor = new ConnectionProcessor("http://server", mockStore, mockDeviceId, null);
        testDeviceId = "123";
    }

    public void testConstructorAndGetters() {
        final String serverURL = "https://secureserver";
        final WigzoStore mockStore = mock(WigzoStore.class);
        final WigzoAppStore wigzoAppStore = mock(WigzoAppStore.class);

        final DeviceId mockDeviceId = mock(DeviceId.class);
        final ConnectionProcessor connectionProcessor1 = new ConnectionProcessor(serverURL, mockStore, mockDeviceId, null);
        assertEquals(serverURL, connectionProcessor1.getServerURL());
        assertSame(mockStore, connectionProcessor1.getWigzoStore());
        assertSame(mockDeviceId, connectionProcessor1.getDeviceId());

        final ConnectionProcessorWigzoApp connectionProcessor2 = new ConnectionProcessorWigzoApp(serverURL, wigzoAppStore, mockDeviceId, null);
        assertEquals(serverURL, connectionProcessor2.getServerURL());
        assertSame(wigzoAppStore, connectionProcessor2.getWigzoAppStore());
        assertSame(mockDeviceId, connectionProcessor2.getDeviceId());
    }

    public void testUrlConnectionForEventData() throws IOException {
        final String eventData = "blahblahblah";
        final URLConnection urlConnection = connectionProcessor.urlConnectionForEventData(eventData);
        assertEquals(30000, urlConnection.getConnectTimeout());
        assertEquals(30000, urlConnection.getReadTimeout());
        assertFalse(urlConnection.getUseCaches());
        assertTrue(urlConnection.getDoInput());
        assertFalse(urlConnection.getDoOutput());
        assertEquals(new URL(connectionProcessor.getServerURL() + "/i?" + eventData), urlConnection.getURL());
    }

    public void testRun_storeReturnsNullConnections() throws IOException {
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(null);
        when(wigzoAppStore.connections()).thenReturn(null);
        connectionProcessor.run();
        verify(mockStore).connections();
        verify(wigzoAppStore).connections();
        verify(connectionProcessor, times(0)).urlConnectionForEventData(anyString());
    }

    public void testRun_storeReturnsEmptyConnections() throws IOException {
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(new String[0]);
        when(wigzoAppStore.connections()).thenReturn(new String[0]);
        connectionProcessor.run();
        verify(mockStore).connections();
        verify(wigzoAppStore).connections();
        verify(connectionProcessor, times(0)).urlConnectionForEventData(anyString());
    }

    private static class TestInputStream extends InputStream {
        int readCount = 0;
        boolean fullyRead() { return readCount >= 2; }
        boolean closed = false;

        @Override
        public int read() throws IOException {
            return readCount++ < 1 ? 1 : -1;
        }

        @Override
        public void close() throws IOException {
            super.close();
            closed = true;
        }
    }

    private static class WigzoResponseStream extends ByteArrayInputStream {
        boolean closed = false;

        WigzoResponseStream(final String result) throws UnsupportedEncodingException {
            super(("{\"result\":\"" + result + "\"}").getBytes("UTF-8"));
        }

        boolean fullyRead() { return pos == buf.length; }

        @Override
        public void close() throws IOException {
            super.close();
            closed = true;
        }
    }

    public void testRun_storeHasSingleConnection() throws IOException {
        final String eventData = "blahblahblah";
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(wigzoAppStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(mockDeviceId.getId()).thenReturn(testDeviceId);
        final HttpURLConnection mockURLConnection = mock(HttpURLConnection.class);
        final WigzoResponseStream testInputStream = new WigzoResponseStream("Success");
        when(mockURLConnection.getInputStream()).thenReturn(testInputStream);
        when(mockURLConnection.getResponseCode()).thenReturn(200);
        doReturn(mockURLConnection).when(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        connectionProcessor.run();
        verify(mockStore, times(2)).connections();
        verify(wigzoAppStore, times(2)).connections();
        verify(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        verify(mockURLConnection).connect();
        verify(mockURLConnection).getInputStream();
        verify(mockURLConnection).getResponseCode();
        assertTrue(testInputStream.fullyRead());
        verify(mockStore).removeConnection(eventData);
        verify(wigzoAppStore).removeConnection(eventData);
        assertTrue(testInputStream.closed);
        verify(mockURLConnection).disconnect();
    }

    public void testRun_storeHasSingleConnection_butHTTPResponseCodeWasNot2xx() throws IOException {
        final String eventData = "blahblahblah";
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(wigzoAppStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(mockDeviceId.getId()).thenReturn(testDeviceId);
        final HttpURLConnection mockURLConnection = mock(HttpURLConnection.class);
        final WigzoResponseStream testInputStream = new WigzoResponseStream("Success");
        when(mockURLConnection.getInputStream()).thenReturn(testInputStream);
        when(mockURLConnection.getResponseCode()).thenReturn(300);
        doReturn(mockURLConnection).when(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        connectionProcessor.run();
        verify(mockStore).connections();
        verify(wigzoAppStore).connections();
        verify(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        verify(mockURLConnection).connect();
        verify(mockURLConnection).getInputStream();
        verify(mockURLConnection).getResponseCode();
        assertTrue(testInputStream.fullyRead());
        verify(mockStore, times(0)).removeConnection(eventData);
        verify(wigzoAppStore, times(0)).removeConnection(eventData);
        assertTrue(testInputStream.closed);
        verify(mockURLConnection).disconnect();
    }

    public void testRun_storeHasSingleConnection_butResponseWasNotJSON() throws IOException {
        final String eventData = "blahblahblah";
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(wigzoAppStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(mockDeviceId.getId()).thenReturn(testDeviceId);
        final HttpURLConnection mockURLConnection = mock(HttpURLConnection.class);
        final TestInputStream testInputStream = new TestInputStream();
        when(mockURLConnection.getInputStream()).thenReturn(testInputStream);
        when(mockURLConnection.getResponseCode()).thenReturn(200);
        doReturn(mockURLConnection).when(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        connectionProcessor.run();
        verify(mockStore).connections();
        verify(wigzoAppStore).connections();
        verify(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        verify(mockURLConnection).connect();
        verify(mockURLConnection).getInputStream();
        verify(mockURLConnection).getResponseCode();
        assertTrue(testInputStream.fullyRead());
        verify(mockStore, times(0)).removeConnection(eventData);
        verify(wigzoAppStore, times(0)).removeConnection(eventData);
        assertTrue(testInputStream.closed);
        verify(mockURLConnection).disconnect();
    }

    public void testRun_storeHasSingleConnection_butResponseJSONWasNotSuccess() throws IOException {
        final String eventData = "blahblahblah";
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(wigzoAppStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(mockDeviceId.getId()).thenReturn(testDeviceId);
        final HttpURLConnection mockURLConnection = mock(HttpURLConnection.class);
        final WigzoResponseStream testInputStream = new WigzoResponseStream("Failed");
        when(mockURLConnection.getInputStream()).thenReturn(testInputStream);
        when(mockURLConnection.getResponseCode()).thenReturn(200);
        doReturn(mockURLConnection).when(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        connectionProcessor.run();
        verify(mockStore).connections();
        verify(wigzoAppStore).connections();
        verify(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        verify(mockURLConnection).connect();
        verify(mockURLConnection).getInputStream();
        assertTrue(testInputStream.fullyRead());
        verify(mockURLConnection).getResponseCode();
        verify(mockStore, times(0)).removeConnection(eventData);
        verify(wigzoAppStore, times(0)).removeConnection(eventData);
        assertTrue(testInputStream.closed);
        verify(mockURLConnection).disconnect();
    }

    public void testRun_storeHasSingleConnection_successCheckIsCaseInsensitive() throws IOException {
        final String eventData = "blahblahblah";
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(wigzoAppStore.connections()).thenReturn(new String[]{eventData}, new String[0]);
        when(mockDeviceId.getId()).thenReturn(testDeviceId);
        final HttpURLConnection mockURLConnection = mock(HttpURLConnection.class);
        final WigzoResponseStream testInputStream = new WigzoResponseStream("SuCcEsS");
        when(mockURLConnection.getInputStream()).thenReturn(testInputStream);
        when(mockURLConnection.getResponseCode()).thenReturn(200);
        doReturn(mockURLConnection).when(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        connectionProcessor.run();
        verify(mockStore, times(2)).connections();
        verify(wigzoAppStore, times(2)).connections();
        verify(connectionProcessor).urlConnectionForEventData(eventData + "&device_id=" + testDeviceId);
        verify(mockURLConnection).connect();
        verify(mockURLConnection).getInputStream();
        verify(mockURLConnection).getResponseCode();
        assertTrue(testInputStream.fullyRead());
        verify(mockStore).removeConnection(eventData);
        verify(wigzoAppStore).removeConnection(eventData);
        assertTrue(testInputStream.closed);
        verify(mockURLConnection).disconnect();
    }

    public void testRun_storeHasTwoConnections() throws IOException {
        final String eventData1 = "blahblahblah";
        final String eventData2 = "123523523432";
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(new String[]{eventData1, eventData2}, new String[]{eventData2}, new String[0]);
        when(wigzoAppStore.connections()).thenReturn(new String[]{eventData1, eventData2}, new String[]{eventData2}, new String[0]);
        when(mockDeviceId.getId()).thenReturn(testDeviceId);
        final HttpURLConnection mockURLConnection = mock(HttpURLConnection.class);
        final WigzoResponseStream testInputStream1 = new WigzoResponseStream("Success");
        final WigzoResponseStream testInputStream2 = new WigzoResponseStream("Success");
        when(mockURLConnection.getInputStream()).thenReturn(testInputStream1, testInputStream2);
        doReturn(mockURLConnection).when(connectionProcessor).urlConnectionForEventData(eventData1 + "&device_id=" + testDeviceId);
        doReturn(mockURLConnection).when(connectionProcessor).urlConnectionForEventData(eventData2 + "&device_id=" + testDeviceId);
        when(mockURLConnection.getResponseCode()).thenReturn(200, 200);
        connectionProcessor.run();
        verify(mockStore, times(3)).connections();
        verify(wigzoAppStore, times(3)).connections();
        verify(connectionProcessor).urlConnectionForEventData(eventData1 + "&device_id=" + testDeviceId);
        verify(connectionProcessor).urlConnectionForEventData(eventData2 + "&device_id=" + testDeviceId);
        verify(mockURLConnection, times(2)).connect();
        verify(mockURLConnection, times(2)).getInputStream();
        verify(mockURLConnection, times(2)).getResponseCode();
        assertTrue(testInputStream1.fullyRead());
        assertTrue(testInputStream2.fullyRead());
        verify(mockStore).removeConnection(eventData1);
        verify(wigzoAppStore).removeConnection(eventData1);
        verify(mockStore).removeConnection(eventData2);
        verify(wigzoAppStore).removeConnection(eventData2);
        assertTrue(testInputStream1.closed);
        assertTrue(testInputStream2.closed);
        verify(mockURLConnection, times(2)).disconnect();
    }

    private static class TestInputStream2 extends InputStream {
        boolean closed = false;

        @Override
        public int read() throws IOException {
            throw new IOException();
        }

        @Override
        public void close() throws IOException {
            super.close();
            closed = true;
        }
    }

    public void testRun_storeHasTwoConnections_butFirstOneThrowsWhenInputStreamIsRead() throws IOException {
        final String eventData1 = "blahblahblah";
        final String eventData2 = "123523523432";
        connectionProcessor = spy(connectionProcessor);
        when(mockStore.connections()).thenReturn(new String[]{eventData1, eventData2}, new String[]{eventData2}, new String[0]);
        when(wigzoAppStore.connections()).thenReturn(new String[]{eventData1, eventData2}, new String[]{eventData2}, new String[0]);
        when(mockDeviceId.getId()).thenReturn(testDeviceId);
        final HttpURLConnection mockURLConnection = mock(HttpURLConnection.class);
        final TestInputStream2 testInputStream = new TestInputStream2();
        when(mockURLConnection.getInputStream()).thenReturn(testInputStream);
        doReturn(mockURLConnection).when(connectionProcessor).urlConnectionForEventData(eventData1 + "&device_id=" + testDeviceId);
        connectionProcessor.run();
        verify(mockStore).connections();
        verify(wigzoAppStore).connections();
        verify(connectionProcessor).urlConnectionForEventData(eventData1 + "&device_id=" + testDeviceId);
        verify(connectionProcessor, times(0)).urlConnectionForEventData(eventData2 + "&device_id=" + testDeviceId);
        verify(mockURLConnection).connect();
        verify(mockURLConnection).getInputStream();
        verify(mockStore, times(0)).removeConnection(anyString());
        verify(wigzoAppStore, times(0)).removeConnection(anyString());
        assertTrue(testInputStream.closed);
        verify(mockURLConnection).disconnect();
    }
}
