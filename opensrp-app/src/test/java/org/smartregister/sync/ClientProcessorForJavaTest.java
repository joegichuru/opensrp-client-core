package org.smartregister.sync;

import android.content.ContentValues;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.BaseUnitTest;
import org.smartregister.CoreLibrary;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.db.Client;
import org.smartregister.domain.db.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.db.Obs;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.domain.jsonmapping.Column;
import org.smartregister.domain.jsonmapping.ColumnType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.smartregister.sync.ClientProcessorForJava.JSON_ARRAY;


public class ClientProcessorForJavaTest extends BaseUnitTest {

    @Mock
    private CoreLibrary coreLibrary;

    @Mock
    private org.smartregister.Context opensrpContext;

    @Mock
    private Context context;

    @Captor
    private ArgumentCaptor<String> closeCaseArgumentCaptor;

    @Test
    public void testGetFormattedValueDate() throws Exception {
        ClientProcessorForJava clientProcessor = new ClientProcessorForJava(context);

        Column column = new Column();
        column.dataType = ColumnType.Date;
        column.saveFormat = "yyyy-MM-dd";
        column.sourceFormat = "dd-MM-yyyy";
        String columnValue = "16-04-2019";

        String res = Whitebox.invokeMethod(clientProcessor, "getFormattedValue", column, columnValue);

        assertEquals(res, "2019-04-16");
    }

    @Test
    public void testGetFormattedValueString() throws Exception {
        ClientProcessorForJava clientProcessor = new ClientProcessorForJava(context);

        Column column = new Column();
        column.dataType = ColumnType.String;
        column.saveFormat = "Sheila is %s";
        String columnValue = "smart";

        String res = Whitebox.invokeMethod(clientProcessor, "getFormattedValue", column, columnValue);

        assertEquals(res, "Sheila is smart");
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testProcessEventMarksItSaved() throws Exception {
        ClientProcessorForJava clientProcessorForJava = Mockito.spy(new ClientProcessorForJava(context));
        Event mockEvent = Mockito.mock(Event.class);
        clientProcessorForJava.processEvent(mockEvent, null, null);

        Mockito.verify(clientProcessorForJava).completeProcessing(mockEvent);
    }

    @Test
    public void testGetValuesStrShouldGetCorrectlyFormattedString() throws Exception {
        ClientProcessorForJava clientProcessor = new ClientProcessorForJava(context);
        List<String> values = new ArrayList<>();
        Obs obs = new Obs();
        obs.withsaveObsAsArray(true);
        String valStr = Whitebox.invokeMethod(clientProcessor, "getValuesStr", obs, values, JSON_ARRAY);
        assertNull(valStr);

        values.add("val1");
        values.add("val2");
        values.add("val3");
        valStr = Whitebox.invokeMethod(clientProcessor, "getValuesStr", obs, values, null);
        assertEquals("[\"val1\",\"val2\",\"val3\"]", valStr);

        obs.setSaveObsAsArray(false);
        valStr = Whitebox.invokeMethod(clientProcessor, "getValuesStr", obs, values, JSON_ARRAY);
        assertEquals("[\"val1\",\"val2\",\"val3\"]", valStr);

        valStr = Whitebox.invokeMethod(clientProcessor, "getValuesStr", obs, values, null);
        assertEquals("val1", valStr);
    }

    @Test
    public void testProcessEventUsingMiniProcessorShouldCallMiniProcessorProcessEventClient() throws Exception {
        MiniClientProcessorForJava miniClientProcessorForJava = Mockito.mock(MiniClientProcessorForJava.class);
        String eventType = "Custom Test Event";
        HashSet<String> supportedEventTypes = new HashSet<>();
        supportedEventTypes.add(eventType);
        Mockito.doReturn(supportedEventTypes).when(miniClientProcessorForJava).getEventTypes();

        String baseEntityId = "bei";
        EventClient eventClient = new EventClient(new Event(), new Client(baseEntityId));

        ClientProcessorForJava clientProcessorForJava = Mockito.spy(new ClientProcessorForJava(context));
        clientProcessorForJava.addMiniProcessors(miniClientProcessorForJava);

        ClientClassification clientClassification = new ClientClassification();
        clientProcessorForJava.processEventUsingMiniProcessor(clientClassification, eventClient, eventType);

        Mockito.verify(miniClientProcessorForJava, Mockito.times(1)).processEventClient(Mockito.eq(eventClient), ArgumentMatchers.<List<Event>>any(), Mockito.eq(clientClassification));
    }

    @Test
    public void testGetClientAttributesShouldReturnRequiredValues() throws Exception {
        ClientProcessorForJava clientProcessor = new ClientProcessorForJava(context);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("national_id", "3434-34");
        attributes.put("drivers-license", "DL-324");
        Client client = new Client("123-23");
        client.setAttributes(attributes);
        Map<String, Object> result = Whitebox.invokeMethod(clientProcessor, "getClientAttributes", client);
        assertEquals(attributes, result);
    }

    @Test
    public void testGetGenderShouldReturnCorrectValue() throws Exception {
        ClientProcessorForJava clientProcessor = new ClientProcessorForJava(context);
        Client client = new Client("123-23");
        client.setGender("Female");
        Map<String, String> resultMap = Whitebox.invokeMethod(clientProcessor, "getGender", client);
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("gender", "Female");
        assertEquals(expectedMap, resultMap);
    }

    @Test
    public void testUpdateIdenitifierShouldRemoveHyphenFromOpenmrsId() throws Exception {
        ClientProcessorForJava clientProcessor = new ClientProcessorForJava(context);
        ContentValues contentValues = new ContentValues();
        contentValues.put("zeir_id", "23-23");
        Whitebox.setInternalState(clientProcessor, "openmrsGenIds", new String[]{"zeir_id"});
        Whitebox.invokeMethod(clientProcessor, "updateIdenitifier", contentValues);
        assertEquals("2323", contentValues.get("zeir_id"));
    }

    @Test
    public void testCloseCaseShouldReturnFalseIfCloseCaseIsEmpty() {
        ClientProcessorForJava clientProcessor = new ClientProcessorForJava(context);
        assertFalse(clientProcessor.closeCase(new Client("1233-2"), new ArrayList<>()));
    }

    @Test
    public void testCloseCaseShouldPassCorrectValuesToCloseCase() {
        ClientProcessorForJava clientProcessor = new ClientProcessorForJava(context);
        CommonRepository commonRepository = Mockito.mock(CommonRepository.class);
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", coreLibrary);
        PowerMockito.when(coreLibrary.context()).thenReturn(opensrpContext);
        PowerMockito.when(opensrpContext.commonrepository("child")).thenReturn(commonRepository);
        assertTrue(clientProcessor.closeCase(new Client("1233-2"), Arrays.asList("child")));
        Mockito.verify(commonRepository).closeCase(closeCaseArgumentCaptor.capture(), closeCaseArgumentCaptor.capture());
        assertEquals("1233-2", closeCaseArgumentCaptor.getAllValues().get(0));
        assertEquals("child", closeCaseArgumentCaptor.getAllValues().get(1));
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", null);
    }
}
