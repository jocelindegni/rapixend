package com.saankaa.rapidxend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saankaa.rapidxend.config.security.JwtUtils;
import com.saankaa.rapidxend.model.Device;
import com.saankaa.rapidxend.model.Peer;
import com.saankaa.rapidxend.repository.IDeviceRepository;
import com.saankaa.rapidxend.service.device.DeviceService;
import com.saankaa.rapidxend.service.device.Exception.*;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
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
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeviceControllerTest {

    private final Logger LOGGER = LoggerFactory.getLogger(DeviceController.class);
    private final String CONNECTED_DEVICE_ID = "connected-device-id-3456789087654";
    private final String TOKEN = "Bearer FAKE_TOKEN";

    @MockBean
    DeviceService deviceService;
    @MockBean
    JwtUtils jwtUtils; // Mock Jwt decoder and return custom device Id
    @LocalServerPort
    private int port;
    private String base_url;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @MockBean
    private IDeviceRepository deviceRepository;

    @BeforeEach
    public void setUp() {
        // Mock methods called in JwtFilter to return custom Connected device
        base_url = "http://localhost:" + port;
        Mockito.doReturn(CONNECTED_DEVICE_ID).when(jwtUtils).getUserId(isA(String.class));
        Mockito.doReturn(true).when(jwtUtils).validate(isA(String.class));

        Device device = new Device();

        device.setId(CONNECTED_DEVICE_ID);
        Mockito.doReturn(Optional.of(device)).when(deviceRepository).findById(CONNECTED_DEVICE_ID);
    }


    @Test
    void getDeviceByIdOrName() throws Exception {
        LOGGER.debug("Test getDeviceByIdOrName endpoint...");

        Device device = new Device();
        device.setId("device123");
        Mockito.doReturn(device).when(deviceService).getDeviceByNameOrId(isA(String.class));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(TOKEN);

        HttpEntity<String> httpEntity = new HttpEntity<>("", httpHeaders);

        ResponseEntity<Device> response = testRestTemplate.exchange(base_url + "/devices/device123", HttpMethod.GET, httpEntity, Device.class);
        assertEquals(200, response.getStatusCode().value());

        assertNotNull(response.getBody());
        assertEquals("device123", response.getBody().getId());
        // Assert method was called with right parameter
        Mockito.verify(deviceService).getDeviceByNameOrId("device123");

        LOGGER.debug("Throw DeviceNotFoundException...");
        Mockito.doThrow(new DeviceNotFoundException("Device not found")).when(deviceService).getDeviceByNameOrId(isA(String.class));
        response = testRestTemplate.exchange(base_url + "/devices/device123", HttpMethod.GET, httpEntity, Device.class);
        assertEquals(404, response.getStatusCode().value());

        LOGGER.debug("Throw IllegalArgumentException...");
        Mockito.doThrow(new IllegalArgumentException()).when(deviceService).getDeviceByNameOrId(isA(String.class));
        response = testRestTemplate.exchange(base_url + "/devices/device123", HttpMethod.GET, httpEntity, Device.class);
        assertEquals(400, response.getStatusCode().value());

        LOGGER.debug("Throw Exception...");
        Mockito.doThrow(new NullPointerException("An error")).when(deviceService).getDeviceByNameOrId(isA(String.class));
        response = testRestTemplate.exchange(base_url + "/devices/device123", HttpMethod.GET, httpEntity, Device.class);
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void nameIsValidAndFree() throws Exception {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> httpEntity;

        LOGGER.debug("Endpoint: checking name validity...");
        Mockito.doReturn(true).when(deviceService).checkIfDeviceNameIsFree(isA(String.class));

        // Entity
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode oNode = mapper.createObjectNode();
        oNode.put("name", "android");
        httpEntity = new HttpEntity<>(mapper.writeValueAsString(oNode), httpHeaders);

        ResponseEntity<String> response = testRestTemplate.postForEntity(base_url + "/devices/name", httpEntity, String.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("true", response.getBody());

        LOGGER.debug("Return false");
        Mockito.doReturn(false).when(deviceService).checkIfDeviceNameIsFree(isA(String.class));

        response = testRestTemplate.postForEntity(base_url + "/devices/name", httpEntity, String.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("false", response.getBody());

        LOGGER.debug("throw InvalidDeviceName Exception...");
        Mockito.doThrow(new InvalidDeviceNameException("Invalid device name exception")).when(deviceService)
                .checkIfDeviceNameIsFree(isA(String.class));

        response = testRestTemplate.postForEntity(base_url + "/devices/name", httpEntity, String.class);
        assertEquals(400, response.getStatusCode().value());


        LOGGER.debug("throw IllegalArgumentException...");
        Mockito.doThrow(new IllegalArgumentException("null parameters")).when(deviceService)
                .checkIfDeviceNameIsFree(isA(String.class));

        response = testRestTemplate.postForEntity(base_url + "/devices/name", httpEntity, String.class);
        assertEquals(400, response.getStatusCode().value());


        LOGGER.debug("throw nullPointerException...");
        Mockito.doThrow(new NullPointerException("null pointer")).when(deviceService)
                .checkIfDeviceNameIsFree(isA(String.class));

        response = testRestTemplate.postForEntity(base_url + "/devices/name", httpEntity, String.class);
        assertEquals(500, response.getStatusCode().value());

    }

    @Test
    void register() throws Exception {
        LOGGER.debug("Test register device endpoint...");
        Device device = new Device();
        device.setName("iphone");
        // Mock service response
        Mockito.doReturn(device).when(deviceService).register(isA(Device.class));

        ResponseEntity<Device> responseEntity = testRestTemplate.postForEntity(base_url + "/devices/register", device, Device.class);
        assertEquals(201, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals("iphone", responseEntity.getBody().getName());

        LOGGER.debug("Throw InvalidDeviceNameException");
        Mockito.doThrow(new InvalidDeviceNameException("Invalid device name exception")).when(deviceService)
                .register(isA(Device.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/register", device, Device.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new InvalidDeviceNameException("Invalid param")).when(deviceService)
                .register(isA(Device.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/register", device, Device.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw NullPointerException");
        Mockito.doThrow(new NullPointerException("null pointer exception")).when(deviceService)
                .register(isA(Device.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/register", device, Device.class);
        assertEquals(500, responseEntity.getStatusCode().value());


    }

    @Test
    void cloneDevice() throws Exception {
        LOGGER.debug("Test device cloning...");

        Device device = new Device();
        device.setName("samsung");
        Mockito.doReturn(device).when(deviceService).clone(isA(String.class), isA(String.class), isA(Device.class));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("oldDeviceId", "oldId");
        rootNode.put("oldDeviceSecretKey", "secret");
        ObjectNode deviceNode = mapper.createObjectNode();
        deviceNode.put("name", "android");
        rootNode.put("newDevice", deviceNode);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> httpEntity = new HttpEntity<>(mapper.writeValueAsString(rootNode), httpHeaders);

        ResponseEntity<Device> responseEntity = testRestTemplate.postForEntity(base_url + "/devices/clone", httpEntity, Device.class);
        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals("samsung", responseEntity.getBody().getName());

        LOGGER.debug("Throw IllegalArgumentException...");
        Mockito.doThrow(new IllegalArgumentException()).when(deviceService).clone(isA(String.class), isA(String.class), isA(Device.class));

        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/clone", httpEntity, Device.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw DeviceNotFoundException...");
        Mockito.doThrow(new DeviceNotFoundException("")).when(deviceService).clone(isA(String.class), isA(String.class), isA(Device.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/clone", httpEntity, Device.class);
        assertEquals(404, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw BadDeviceSecretKeyException...");
        Mockito.doThrow(new BadDeviceSecretKeyException("")).when(deviceService).clone(isA(String.class), isA(String.class), isA(Device.class));

        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/clone", httpEntity, Device.class);
        assertEquals(403, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw NPE exception...");
        Mockito.doThrow(new NullPointerException("")).when(deviceService).clone(isA(String.class), isA(String.class), isA(Device.class));

        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/clone", httpEntity, Device.class);
        assertEquals(500, responseEntity.getStatusCode().value());


    }

    @Test
    void getPeers() {
        LOGGER.debug("Test get peers...");
        Device d1 = new Device(), d2 = new Device();
        d1.setName("iphone1");
        d2.setName("iphone2");

        Mockito.doReturn(new ArrayList<Device>() {{
            add(d1);
            add(d2);
        }}).when(deviceService).getPeerDevices(isA(String.class));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(TOKEN);
        ResponseEntity<Device[]> responseEntity = testRestTemplate.exchange(base_url + "/devices/peer", HttpMethod.GET, new HttpEntity<>(httpHeaders), Device[].class);
        assertEquals(200, responseEntity.getStatusCode().value());

        Mockito.verify(deviceService).getPeerDevices(CONNECTED_DEVICE_ID);

        assertNotNull(responseEntity.getBody());
        assertTrue(Arrays.stream(responseEntity.getBody()).anyMatch(device -> device.getName().equalsIgnoreCase("iphone1")));
        assertTrue(Arrays.stream(responseEntity.getBody()).anyMatch(device -> device.getName().equalsIgnoreCase("iphone2")));

        // Assert method called with right peer
        Mockito.verify(deviceService).getPeerDevices(CONNECTED_DEVICE_ID);

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException("")).when(deviceService).getPeerDevices(isA(String.class));
        ResponseEntity<Object> responseEntityObject = testRestTemplate.exchange(base_url + "/devices/peer", HttpMethod.GET, new HttpEntity<>(httpHeaders), Object.class);
        assertEquals(400, responseEntityObject.getStatusCode().value());

        LOGGER.debug("Throw NPE");
        Mockito.doThrow(new NullPointerException("")).when(deviceService).getPeerDevices(isA(String.class));
        responseEntityObject = testRestTemplate.exchange(base_url + "/devices/peer", HttpMethod.GET, new HttpEntity<>(httpHeaders), Object.class);
        assertEquals(500, responseEntityObject.getStatusCode().value());
    }

    @Test
    void peerRequest() throws Exception {

        LOGGER.debug("Test peering request...");

        Peer peer = new Peer();
        peer.setId("id123");

        Mockito.doReturn(peer).when(deviceService).peering(isA(String.class), isA(String.class));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setBearerAuth(TOKEN);
        HttpEntity<String> httpEntity = new HttpEntity<>("{\"applicantDeviceId\":\"applicant1234\"}", httpHeaders);

        ResponseEntity<Peer> responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer", httpEntity, Peer.class);
        assertEquals(201, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals("id123", responseEntity.getBody().getId());

        // Assert that deviceService was called with right parameters
        Mockito.verify(deviceService).peering(CONNECTED_DEVICE_ID, "applicant1234");

        LOGGER.debug("Throw IllegalArgumentException");

        Mockito.doThrow(new IllegalArgumentException()).when(deviceService).peering(isA(String.class), isA(String.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer", httpEntity, Peer.class);
        assertEquals(400, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw DeviceNotFoundException");

        Mockito.doThrow(new DeviceNotFoundException("dne")).when(deviceService).peering(isA(String.class), isA(String.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer", httpEntity, Peer.class);
        assertEquals(404, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw PeerConflictException");

        Mockito.doThrow(new PeerConflictException("")).when(deviceService).peering(isA(String.class), isA(String.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer", httpEntity, Peer.class);
        assertEquals(409, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw NPE");

        Mockito.doThrow(new NullPointerException("")).when(deviceService).peering(isA(String.class), isA(String.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer", httpEntity, Peer.class);
        assertEquals(500, responseEntity.getStatusCode().value());


    }

    @Test
    void acceptPeerRequest() throws Exception {

        LOGGER.debug("Test accept peer request");

        Mockito.doNothing().when(deviceService).acceptPeering(isA(String.class), isA(String.class), isA(Boolean.class));
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("requesterId", "requester123");
        node.put("accept", true);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setBearerAuth(TOKEN);
        HttpEntity<String> httpEntity = new HttpEntity<>(mapper.writeValueAsString(node), httpHeaders);

        ResponseEntity<String> responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer/accept", httpEntity, String.class);
        assertEquals(200, responseEntity.getStatusCode().value());

        Mockito.verify(deviceService).acceptPeering("requester123", CONNECTED_DEVICE_ID, true);

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException()).when(deviceService).acceptPeering(isA(String.class), isA(String.class), isA(Boolean.class));

        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer/accept", httpEntity, String.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw DeviceNotFoundException");

        Mockito.doThrow(new DeviceNotFoundException("")).when(deviceService).acceptPeering(isA(String.class), isA(String.class), isA(Boolean.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer/accept", httpEntity, String.class);
        assertEquals(404, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw NPE");

        Mockito.doThrow(new NullPointerException("")).when(deviceService).acceptPeering(isA(String.class), isA(String.class), isA(Boolean.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/peer/accept", httpEntity, String.class);
        assertEquals(500, responseEntity.getStatusCode().value());


    }

    @Test
    void dissociate() {

        LOGGER.debug("Test dissociate endpoint");

        Mockito.doNothing().when(deviceService).dissociate(isA(String.class), isA(String.class));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(TOKEN);
        ResponseEntity<String> responseEntity = testRestTemplate.exchange(base_url + "/devices/peer/12345", HttpMethod.DELETE, new HttpEntity<>("", httpHeaders), String.class);
        assertEquals(200, responseEntity.getStatusCode().value());

        Mockito.verify(deviceService).dissociate(CONNECTED_DEVICE_ID, "12345");

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException("")).when(deviceService).dissociate(isA(String.class), isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/devices/peer/12345", HttpMethod.DELETE, new HttpEntity<>("", httpHeaders), String.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw NPE");
        Mockito.doThrow(new NullPointerException("")).when(deviceService).dissociate(isA(String.class), isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/devices/peer/12345", HttpMethod.DELETE, new HttpEntity<>("", httpHeaders), String.class);
        assertEquals(500, responseEntity.getStatusCode().value());

    }

    @Test
    void getDevicePhoto() throws Exception {
        LOGGER.debug("Test get photo...");

        byte[] data = new byte[1024];
        new Random().nextBytes(data);

        Mockito.doReturn(data).when(deviceService).getPhoto(isA(String.class));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(TOKEN);
        ResponseEntity<byte[]> responseEntity = testRestTemplate.exchange(base_url + "/devices/photo/id1234", HttpMethod.GET, new HttpEntity<>(httpHeaders), byte[].class);
        assertEquals(200, responseEntity.getStatusCode().value());
        assertTrue(Arrays.equals(data, responseEntity.getBody()));

        Mockito.verify(deviceService).getPhoto("id1234");

        LOGGER.debug("Throw IllegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException()).when(deviceService).getPhoto(isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/devices/photo/id1234", HttpMethod.GET, new HttpEntity<>(httpHeaders), byte[].class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw DeviceNotFoundException");
        Mockito.doThrow(new DeviceNotFoundException("dne")).when(deviceService).getPhoto(isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/devices/photo/id1234", HttpMethod.GET, new HttpEntity<>(httpHeaders), byte[].class);
        assertEquals(404, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw NPE");
        Mockito.doThrow(new NullPointerException()).when(deviceService).getPhoto(isA(String.class));
        responseEntity = testRestTemplate.exchange(base_url + "/devices/photo/id1234", HttpMethod.GET, new HttpEntity<>(httpHeaders), byte[].class);
        assertEquals(500, responseEntity.getStatusCode().value());

    }

    @Test
    void updateDevicePhoto() throws Exception {

        LOGGER.debug("Test device photo update");

        Mockito.doNothing().when(deviceService).updatePhoto(isA(String.class), isA(MultipartFile.class));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileAsResource = new ByteArrayResource("photo".getBytes()) {
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

        ResponseEntity<Object> responseEntity = testRestTemplate.postForEntity(base_url + "/devices/photo", multipartFileHttpEntity, Object.class);
        assertEquals(200, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw illegalArgumentException");
        Mockito.doThrow(new IllegalArgumentException()).when(deviceService).updatePhoto(isA(String.class), isA(MultipartFile.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/photo", multipartFileHttpEntity, Object.class);
        assertEquals(400, responseEntity.getStatusCode().value());

        LOGGER.debug("Throw InvalidFileType");
        Mockito.doThrow(new InvalidFileType("ift")).when(deviceService).updatePhoto(isA(String.class), isA(MultipartFile.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/photo", multipartFileHttpEntity, Object.class);
        assertEquals(400, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw FileTooLargeException");
        Mockito.doThrow(new FileTooLargeException("ftl")).when(deviceService).updatePhoto(isA(String.class), isA(MultipartFile.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/photo", multipartFileHttpEntity, Object.class);
        assertEquals(413, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw DeviceNotFoundException");
        Mockito.doThrow(new DeviceNotFoundException("dne")).when(deviceService).updatePhoto(isA(String.class), isA(MultipartFile.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/photo", multipartFileHttpEntity, Object.class);
        assertEquals(404, responseEntity.getStatusCode().value());


        LOGGER.debug("Throw NPE");
        Mockito.doThrow(new NullPointerException()).when(deviceService).updatePhoto(isA(String.class), isA(MultipartFile.class));
        responseEntity = testRestTemplate.postForEntity(base_url + "/devices/photo", multipartFileHttpEntity, Object.class);
        assertEquals(500, responseEntity.getStatusCode().value());


    }
}