package com.saankaa.rapidxend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saankaa.rapidxend.config.security.JwtUtils;
import com.saankaa.rapidxend.model.Device;
import com.saankaa.rapidxend.model.File;
import com.saankaa.rapidxend.model.Transfer;
import com.saankaa.rapidxend.model.TransferState;
import com.saankaa.rapidxend.repository.IDeviceRepository;
import com.saankaa.rapidxend.service.device.Exception.DeviceNotFoundException;
import com.saankaa.rapidxend.service.transfer.TransferService;
import com.saankaa.rapidxend.service.transfer.exception.FileCorruptedException;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import com.saankaa.rapidxend.service.transfer.exception.InvalidDataBlockException;
import com.saankaa.rapidxend.service.transfer.exception.TransferNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferControllerTest {

    private final Logger LOGGER = LoggerFactory.getLogger(TransferControllerTest.class);
    private final String TOKEN = "Bearer FAKE_TOKEN";
    private final String CONNECTED_DEVICE_ID = "authenticated-device-id-987678998";
    @MockBean
    TransferService transferService;
    @MockBean
    JwtUtils jwtUtils;
    @MockBean
    IDeviceRepository deviceRepository;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @LocalServerPort
    private int port;
    private String base_url;

    @BeforeEach
    public void setUp() {
        base_url = "http://localhost:" + port + "/transfers";

        // Mock methods called in JwtFilter to return custom Connected device
        Mockito.doReturn(CONNECTED_DEVICE_ID).when(jwtUtils).getUserId(isA(String.class));
        Mockito.doReturn(true).when(jwtUtils).validate(isA(String.class));

        Device device = new Device();

        device.setId(CONNECTED_DEVICE_ID);
        Mockito.doReturn(Optional.of(device)).when(deviceRepository).findById(CONNECTED_DEVICE_ID);
    }

    @Test
    void getAllTransfers() throws Exception {

        LOGGER.debug("Get all transfers testing endpoint");
        Transfer transfer = new Transfer();
        transfer.setId("id0102");
        List<Transfer> transfers = new ArrayList<>() {{
            add(transfer);
        }};

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(TOKEN);
        Mockito.doReturn(transfers).when(transferService).getTransfers(isA(String.class), isA(Pageable.class));
        ResponseEntity<Transfer[]> responseEntity = testRestTemplate.exchange(base_url, HttpMethod.GET, new HttpEntity<>(httpHeaders), Transfer[].class);
        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals("id0102", responseEntity.getBody()[0].getId());
        Mockito.verify(transferService).getTransfers("id", isA(Pageable.class));

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException("")).when(transferService).getTransfers(isA(String.class), isA(Pageable.class));
        responseEntity = testRestTemplate.exchange(base_url, HttpMethod.GET, new HttpEntity<>(httpHeaders), Transfer[].class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw DeviceNotFoundException");
        Mockito.doThrow(new DeviceNotFoundException("")).when(transferService).getTransfers(isA(String.class), isA(Pageable.class));
        responseEntity = testRestTemplate.exchange(base_url, HttpMethod.GET, new HttpEntity<>(httpHeaders), Transfer[].class);
        assertEquals(404, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw NPE");
        Mockito.doThrow(new NullPointerException("")).when(transferService).getTransfers(isA(String.class), isA(Pageable.class));
        responseEntity = testRestTemplate.exchange(base_url, HttpMethod.GET, new HttpEntity<>(httpHeaders), Transfer[].class);
        assertEquals(500, responseEntity.getStatusCode().value());

    }

    @Test
    void getInProgressTransfers() {
        LOGGER.debug("Get all transfers testing endpoint");
        Transfer transferInProgress = new Transfer(), finishedTransfer = new Transfer();
        transferInProgress.setId("id0102");
        transferInProgress.setState(TransferState.IN_PROGRESS);

        List<Transfer> transfers = new ArrayList<>() {{
            add(transferInProgress);
            add(finishedTransfer);
        }};
        Mockito.doReturn(transfers).when(transferService).getInProgressTransfers(isA(String.class));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(TOKEN);
        ResponseEntity<Transfer[]> responseEntity = testRestTemplate.exchange(base_url + "/inProgress", HttpMethod.GET, new HttpEntity<>(httpHeaders), Transfer[].class);
        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertTrue(Arrays.stream(responseEntity.getBody()).anyMatch(transfer -> transfer.getId().equals(transferInProgress.getId())));
        assertTrue(Arrays.stream(responseEntity.getBody()).noneMatch(transfer -> transfer.getId().equals(finishedTransfer.getId())));

        Mockito.verify(transferService).getInProgressTransfers(CONNECTED_DEVICE_ID);

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException("")).when(transferService).getInProgressTransfers(isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/inProgress", HttpMethod.GET, new HttpEntity<>(httpHeaders), Transfer[].class);
        assertEquals(400, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw NPE");
        Mockito.doThrow(new NullPointerException("")).when(transferService).getInProgressTransfers(isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/inProgress", HttpMethod.GET, new HttpEntity<>(httpHeaders), Transfer[].class);
        assertEquals(500, responseEntity.getStatusCode().value());
    }

    @Test
    void createTransfer() throws Exception {
        LOGGER.debug("Test create transfer");
        final ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> body = new HashMap<>();
        body.put("file", new File());
        body.put("receiverIds", new ArrayList<>() {{
            add("id-1");
            add("id-2");
        }});

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(TOKEN);
        HttpEntity<String> httpEntity = new HttpEntity<>(mapper.writeValueAsString(body), httpHeaders);

        Transfer transfer = new Transfer();
        transfer.setId("id123");
        Mockito.doReturn(transfer).when(transferService).create(isA(String.class), isA(Transfer.class), isA(List.class));
        ResponseEntity<Transfer> responseEntity = testRestTemplate.postForEntity(base_url, httpEntity, Transfer.class);
        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals("id123", responseEntity.getBody().getId());

        Mockito.verify(transferService).create(CONNECTED_DEVICE_ID, isA(Transfer.class), isA(List.class));

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException()).when(transferService).create(isA(String.class), isA(Transfer.class), isA(List.class));
        responseEntity = testRestTemplate.postForEntity(base_url, httpEntity, Transfer.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new FileTooLargeException("")).when(transferService).create(isA(String.class), isA(Transfer.class), isA(List.class));
        responseEntity = testRestTemplate.postForEntity(base_url, httpEntity, Transfer.class);
        assertEquals(413, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw DeviceNotFound");
        Mockito.doThrow(new DeviceNotFoundException("")).when(transferService).create(isA(String.class), isA(Transfer.class), isA(List.class));
        responseEntity = testRestTemplate.postForEntity(base_url, httpEntity, Transfer.class);
        assertEquals(404, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw NPE");
        Mockito.doThrow(new NullPointerException()).when(transferService).create(isA(String.class), isA(Transfer.class), isA(List.class));
        responseEntity = testRestTemplate.postForEntity(base_url, httpEntity, Transfer.class);
        assertEquals(500, responseEntity.getStatusCode().value());

    }

    @Test
    void cancelTransfer() throws Exception {
        LOGGER.debug("Test cancel transfer");

        Mockito.doNothing().when(transferService).cancel(isA(String.class), isA(String.class));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(TOKEN);
        ResponseEntity<Object> responseEntity = testRestTemplate.exchange(base_url + "/id12345", HttpMethod.DELETE, new HttpEntity<>("", httpHeaders), Object.class);
        assertEquals(200, responseEntity.getStatusCode().value());
        Mockito.verify(transferService).cancel("id12345", CONNECTED_DEVICE_ID);

        LOGGER.debug("Throw IllegalArgumentException");
        responseEntity = testRestTemplate.exchange(base_url + "/id12345", HttpMethod.DELETE, new HttpEntity<>("", httpHeaders), Object.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw DeviceNotFoundException");
        Mockito.doThrow(new DeviceNotFoundException("")).when(transferService).cancel(isA(String.class), isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/id12345", HttpMethod.DELETE, new HttpEntity<>("", httpHeaders), Object.class);
        assertEquals(404, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw NPE");
        Mockito.doThrow(new NullPointerException()).when(transferService).cancel(isA(String.class), isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/id12345", HttpMethod.DELETE, new HttpEntity<>("", httpHeaders), Object.class);
        assertEquals(500, responseEntity.getStatusCode().value());


    }

    @Test
    void sendData() throws Exception {

        LOGGER.debug("Test send data");

        Mockito.doReturn(666).when(transferService).sendData(isA(String.class), isA(String.class), isA(byte[].class));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileAsResource = new ByteArrayResource("data".getBytes()) {
            @Override
            public String getFilename() {
                return "photo.png";
            }
        };
        body.add("file", fileAsResource);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", MediaType.MULTIPART_FORM_DATA_VALUE);
        httpHeaders.setBearerAuth(TOKEN);
        HttpEntity<MultiValueMap> multipartFileHttpEntity = new HttpEntity<>(body, httpHeaders);

        ResponseEntity<String> responseEntity = testRestTemplate.postForEntity(base_url + "/t2120/send", httpHeaders, String.class);
        assertEquals(200, responseEntity.getStatusCode().value());
        assertEquals("666", responseEntity.getBody());

        Mockito.verify(transferService).sendData("t2120", CONNECTED_DEVICE_ID, "data".getBytes());


        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException()).when(transferService).sendData(isA(String.class), isA(String.class), isA(byte[].class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t2120/send", httpHeaders, String.class);
        assertEquals(400, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw DeviceNotFoundException");
        Mockito.doThrow(new DeviceNotFoundException("")).when(transferService).sendData(isA(String.class), isA(String.class), isA(byte[].class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t2120/send", httpHeaders, String.class);
        assertEquals(404, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw TransferNotFoundException");
        Mockito.doThrow(new TransferNotFoundException("")).when(transferService).sendData(isA(String.class), isA(String.class), isA(byte[].class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t2120/send", httpHeaders, String.class);
        assertEquals(404, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw FileCorruptedException");
        Mockito.doThrow(new FileCorruptedException("")).when(transferService).sendData(isA(String.class), isA(String.class), isA(byte[].class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t2120/send", httpHeaders, String.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw FileTooLarge");
        Mockito.doThrow(new FileTooLargeException("")).when(transferService).sendData(isA(String.class), isA(String.class), isA(byte[].class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t2120/send", httpHeaders, String.class);
        assertEquals(413, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw Exception");
        Mockito.doThrow(new RuntimeException("")).when(transferService).sendData(isA(String.class), isA(String.class), isA(byte[].class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t2120/send", httpHeaders, String.class);
        assertEquals(500, responseEntity.getStatusCode().value());


    }

    @Test
    void receiveData() throws Exception {

        LOGGER.debug("Test receive data endpoint");

        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        Mockito.doReturn(data).when(transferService).receiveData(isA(String.class), isA(String.class), isA(Integer.class));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(TOKEN);
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(data, httpHeaders);

        ResponseEntity<byte[]> responseEntity = testRestTemplate.postForEntity(base_url + "/t01", httpEntity, byte[].class);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertTrue(Arrays.equals(data, responseEntity.getBody()));
        Mockito.verify(transferService).receiveData("t01", CONNECTED_DEVICE_ID, 102);

        LOGGER.debug("Throw IllegalArgumentException");

        Mockito.doThrow(new IllegalArgumentException()).when(transferService).receiveData(isA(String.class), isA(String.class), isA(Integer.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t01", httpEntity, byte[].class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw DeviceNotFoundException");

        Mockito.doThrow(new DeviceNotFoundException("")).when(transferService).receiveData(isA(String.class), isA(String.class), isA(Integer.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t01", httpEntity, byte[].class);
        assertEquals(404, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw InvalidDataBlockException");

        Mockito.doThrow(new InvalidDataBlockException("")).when(transferService).receiveData(isA(String.class), isA(String.class), isA(Integer.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t01", httpEntity, byte[].class);
        assertEquals(400, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw TransferNotFoundException");

        Mockito.doThrow(new TransferNotFoundException("")).when(transferService).receiveData(isA(String.class), isA(String.class), isA(Integer.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t01", httpEntity, byte[].class);
        assertEquals(404, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw NPE");

        Mockito.doThrow(new NullPointerException()).when(transferService).receiveData(isA(String.class), isA(String.class), isA(Integer.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/t01", httpEntity, byte[].class);
        assertEquals(500, responseEntity.getStatusCode().value());


    }
}