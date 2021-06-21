package com.saankaa.rapidxend.service.device;

import com.saankaa.rapidxend.config.security.JwtUtils;
import com.saankaa.rapidxend.model.*;
import com.saankaa.rapidxend.repository.IDeviceRepository;
import com.saankaa.rapidxend.repository.IPeerRepository;
import com.saankaa.rapidxend.service.device.Exception.*;
import com.saankaa.rapidxend.service.notification.NotificationService;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;


@SpringBootTest
@RunWith(SpringRunner.class)
class IDeviceServiceTest {

    private final Logger LOGGER = LoggerFactory.getLogger(IDeviceServiceTest.class);


    @Autowired
    IDeviceRepository deviceRepository;

    @Autowired
    IPeerRepository peerRepository;

    @Autowired
    IDeviceService deviceService;

    @MockBean
    NotificationService notificationService;


    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
        peerRepository.deleteAll();
    }

    @Test
    @WithMockUser(value = "connected-device-12345", authorities = {"USER"})
    void getDeviceByNameOrId() throws DeviceNotFoundException {
        LOGGER.debug("Test with null name");
        assertThrows(IllegalArgumentException.class, () -> deviceService.getDeviceByNameOrId(null));

        LOGGER.debug("Test with non existent device name");
        assertThrows(DeviceNotFoundException.class, () -> deviceService.getDeviceByNameOrId("device990"));

        Device device = new Device();
        device.setName("jocelin");
        deviceRepository.save(device);
        assertEquals("jocelin", deviceService.getDeviceByNameOrId("JOcelin").getName());
        assertEquals("jocelin", deviceService.getDeviceByNameOrId(device.getId()).getName());
    }

    @Test
    void checkIfDeviceNameIsFree() throws InvalidDeviceNameException {

        LOGGER.debug("Test checkIfDeviceNameIsFree method");
        LOGGER.debug("Try checking with null device name");
        assertThrows(IllegalArgumentException.class, () -> deviceService.checkIfDeviceNameIsFree(null));

        LOGGER.debug("Try checking with empty device name");
        assertThrows(IllegalArgumentException.class, () -> deviceService.checkIfDeviceNameIsFree(""));

        LOGGER.debug("Try with invalid device name(Name begin by non alpha character)");
        assertThrows(InvalidDeviceNameException.class, () -> deviceService.checkIfDeviceNameIsFree("99Jocelin"));
        assertThrows(InvalidDeviceNameException.class, () -> deviceService.checkIfDeviceNameIsFree("_Jocelin"));
        assertThrows(InvalidDeviceNameException.class, () -> deviceService.checkIfDeviceNameIsFree("@Jocelin"));

        assertThrows(InvalidDeviceNameException.class, () -> deviceService.checkIfDeviceNameIsFree("Jocelin__Jocelin"));

        LOGGER.debug("Test valid device name");
        assertTrue(deviceService.checkIfDeviceNameIsFree("jocelin__"));
        assertTrue(deviceService.checkIfDeviceNameIsFree("jocelin78"));
        assertTrue(deviceService.checkIfDeviceNameIsFree("jocelin"));

    }

    @Test
    void register() throws InvalidDeviceNameException {
        LOGGER.debug("Try with null device object");
        assertThrows(IllegalArgumentException.class, () -> deviceService.register(null));

        LOGGER.debug("Save new device");
        Device device = new Device();
        device.setName("JOCelin21");
        device.setBrand("Android");
        device.setModel("Nokia");
        Device deviceSave = deviceService.register(device);
        assertNotNull(deviceSave.getId());
        assertEquals("jocelin21", deviceSave.getName());
        assertEquals("Android", deviceSave.getBrand());
        assertEquals("Nokia", deviceSave.getModel());
        assertNotNull(deviceSave.getSecretKey());
        assertNotNull(deviceSave.getCreatedDate());
    }

    @Test
    void testClone() throws DeviceNotFoundException, BadDeviceSecretKeyException {
        LOGGER.debug("Test with null old device id");
        assertThrows(IllegalArgumentException.class, () -> deviceService.clone(null, "oldSecretKey", new Device()));

        LOGGER.debug("Test with null old device secret key");
        assertThrows(IllegalArgumentException.class, () -> deviceService.clone("device-id", null, new Device()));

        LOGGER.debug("Test with null new device object");
        assertThrows(IllegalArgumentException.class, () -> deviceService.clone("device-id", "oldSecretKey", null));

        Device device = new Device();
        device.setName("jocelin");
        device.setBrand("android");
        device.setModel("Nokia");
        device.setSecretKey("secret-key");
        device = deviceRepository.save(device);

        LOGGER.debug("Test with invalid device id");
        assertThrows(DeviceNotFoundException.class, () -> deviceService.clone("invalid-id", "secret-key", new Device()));

        LOGGER.debug("Test with bad secret key");
        final String deviceId = device.getId();
        assertThrows(BadDeviceSecretKeyException.class, () -> deviceService.clone(deviceId, "bad-secret", new Device()));

        LOGGER.debug("Test with right value ");
        Device newDevice = new Device();
        newDevice.setModel("Ios");
        newDevice.setBrand("Apple");
        deviceService.clone(device.getId(), "secret-key", newDevice);

        Device deviceUpdated = deviceRepository.findByName("jocelin");
        assertNotNull(deviceUpdated);
        assertEquals("Ios", deviceUpdated.getModel());
        assertEquals("Apple", deviceUpdated.getBrand());
        assertNotNull(deviceUpdated.getSecretKey());
        assertNotEquals("secret-key", deviceUpdated.getSecretKey());
    }

    @Test
    void getPeerDevices() {
        // Create 4 devices
        Device deviceA = new Device(), deviceB = new Device(), deviceC = new Device(), deviceD = new Device();
        deviceA.setName("A");
        deviceB.setName("B");
        deviceC.setName("C");
        deviceD.setName("D");
        deviceRepository.save(deviceA);
        deviceRepository.save(deviceB);
        deviceRepository.save(deviceC);
        deviceRepository.save(deviceD);

        // Create 3 peers
        Peer peerAB = new Peer(), peerCA = new Peer(), peerAD = new Peer();

        peerAB.setRequesterDevice(deviceA);
        peerAB.setApplicantDevice(deviceB);
        peerAB.setAccepted(true);
        peerRepository.save(peerAB);

        peerCA.setRequesterDevice(deviceC);
        peerCA.setApplicantDevice(deviceA);
        peerCA.setAccepted(true);
        peerRepository.save(peerCA);

        // Accepted = false for peer between A and D
        peerAD.setRequesterDevice(deviceA);
        peerAD.setApplicantDevice(deviceD);
        peerAD.setAccepted(false);
        peerRepository.save(peerAD);

        assertEquals(2, deviceService.getPeerDevices(deviceA.getId()).size());
        assertTrue(deviceService.getPeerDevices(deviceA.getId()).stream().anyMatch(device -> device.getId().equals(deviceB.getId())));
        assertTrue(deviceService.getPeerDevices(deviceA.getId()).stream().anyMatch(device -> device.getId().equals(deviceC.getId())));

    }

    @Test
    void peering() throws DeviceNotFoundException, PeerConflictException {
        LOGGER.debug("Test with null requester id");
        assertThrows(IllegalArgumentException.class, () -> deviceService.peering(null, "device-id"));
        LOGGER.debug("Test with null applicant device id");
        assertThrows(IllegalArgumentException.class, () -> deviceService.peering("device-id", null));

        Device requesterDevice = new Device(), applicantDevice = new Device();
        applicantDevice.setName("applicant");
        requesterDevice.setName("requester");
        deviceRepository.save(requesterDevice);
        deviceRepository.save(applicantDevice);

        LOGGER.debug("Test with invalid requester id");
        final String applicantId = applicantDevice.getId();
        assertThrows(DeviceNotFoundException.class, () -> deviceService.peering("invalid-id", applicantId));

        LOGGER.debug("Test with invalid applicant id");
        final String requesterId = requesterDevice.getId();
        assertThrows(DeviceNotFoundException.class, () -> deviceService.peering(requesterId, "invalid-id"));

        LOGGER.debug("Test with valid value");
        doNothing().when(notificationService).notifyDevice(isA(Notification.class));
        deviceService.peering(requesterDevice.getId(), applicantDevice.getId());
        Peer peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(requesterId, applicantId);
        assertNotNull(peer);
        assertFalse(peer.isAccepted());
        Date oldDate = peer.getCreatedDate();
        assertNotNull(oldDate);
        LOGGER.debug("Assert peer has been notified");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .senderDeviceId(peer.getRequesterDevice().getId())
                .receiverDeviceId(peer.getApplicantDevice().getId())
                .notificationType(NotificationType.PEERING_REQUEST.getValue()).build());

        LOGGER.debug("Try to create existing peering. Old peer date must be updated");
        deviceService.peering(requesterId, applicantId);
        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(requesterId, applicantId);
        assertEquals(1, peer.getCreatedDate().compareTo(oldDate));
        assertFalse(peer.isAccepted());
        LOGGER.debug("Create peer by the applicant must be updated peer <accepted> attribute to true");
        deviceService.peering(applicantId, requesterId);
        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(requesterId, applicantId);
        assertTrue(peer.isAccepted());
        LOGGER.debug("Try to create existent peering (accepted==true)");
        assertThrows(PeerConflictException.class, () -> deviceService.peering(requesterId, applicantId));

    }

    @Test
    void acceptPeering() throws DeviceNotFoundException {
        Device requesterDevice = new Device(), applicantDevice = new Device();
        applicantDevice.setName("applicant");
        requesterDevice.setName("requester");
        deviceRepository.save(requesterDevice);
        deviceRepository.save(applicantDevice);

        LOGGER.debug("Try with null requester id");
        assertThrows(IllegalArgumentException.class, () -> deviceService.acceptPeering(null, applicantDevice.getId(), true));

        LOGGER.debug("Try with null applicant id");
        assertThrows(IllegalArgumentException.class, () -> deviceService.acceptPeering(requesterDevice.getId(), null, true));

        LOGGER.debug("Test with null requester device id");
        final String applicantId = applicantDevice.getId();
        assertThrows(DeviceNotFoundException.class, () -> deviceService.acceptPeering("invalid-id", applicantId, true));

        LOGGER.debug("Test with null applicant device id");
        final String requestId = requesterDevice.getId();
        assertThrows(DeviceNotFoundException.class, () -> deviceService.acceptPeering(requestId, "invalid-id", true));

        LOGGER.debug("Create valid peering");
        Peer peer = new Peer();
        peer.setAccepted(false);
        peer.setRequesterDevice(requesterDevice);
        peer.setApplicantDevice(applicantDevice);
        peerRepository.save(peer);

        LOGGER.debug("Test accept peering by the requester");
        assertThrows(DeviceNotFoundException.class, () -> deviceService.acceptPeering(applicantId, requestId, true));

        LOGGER.debug("Accept peering");
        doNothing().when(notificationService).notifyDevice(isA(Notification.class));
        deviceService.acceptPeering(requestId, applicantId, true);
        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(requestId, applicantId);
        assertNotNull(peer);
        assertEquals(requestId, peer.getRequesterDevice().getId());
        assertEquals(applicantId, peer.getApplicantDevice().getId());
        assertTrue(peer.isAccepted());

        LOGGER.debug("Assert requester is notified that applicant accept his request");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .senderDeviceId(peer.getApplicantDevice().getId())
                .receiverDeviceId(peer.getRequesterDevice().getId())
                .notificationType(NotificationType.PEERING_ACCEPTED.getValue()).build());


        LOGGER.debug("Denied peering");
        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(requestId, applicantId);
        peer.setAccepted(false);
        peerRepository.save(peer);
        deviceService.acceptPeering(requestId, applicantId, false);
        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(requestId, applicantId);
        assertNull(peer);

        LOGGER.debug("Assert that requester has been notified");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .senderDeviceId(applicantDevice.getId())
                .receiverDeviceId(requesterDevice.getId())
                .notificationType(NotificationType.PEERING_ACCEPTED.getValue()).build());
    }

    @Test
    void dissociate() {
        LOGGER.debug("Dissociate test begin...");
        doNothing().when(notificationService).notifyDevice(isA(Notification.class));

        LOGGER.debug("Create 2 devices. deviceA and deviceB");
        Device deviceA = new Device(), deviceB = new Device();
        deviceRepository.save(deviceA);
        deviceRepository.save(deviceB);

        LOGGER.debug("Test with null device A id");
        assertThrows(IllegalArgumentException.class, () -> deviceService.dissociate(null, deviceB.getId()));

        LOGGER.debug("Test with null device B id");
        assertThrows(IllegalArgumentException.class, () -> deviceService.dissociate(deviceA.getId(), null));

        LOGGER.debug("peering device1 and deviceB");
        Peer peer = new Peer();
        peer.setRequesterDevice(deviceA);
        peer.setApplicantDevice(deviceB);
        peerRepository.save(peer);

        LOGGER.debug("Dissociating by deviceA..");
        deviceService.dissociate(deviceA.getId(), deviceB.getId());
        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(deviceA.getId(), deviceB.getId());
        assertNull(peer);

        LOGGER.debug("Assert peer has been notified");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .senderDeviceId(deviceA.getId())
                .receiverDeviceId(deviceB.getId())
                .notificationType(NotificationType.DISSOCIATED.getValue()).build());

        LOGGER.debug("Dissociating by deviceB..");
        peer = new Peer();
        peer.setRequesterDevice(deviceA);
        peer.setApplicantDevice(deviceB);
        peerRepository.save(peer);
        deviceService.dissociate(deviceB.getId(), deviceA.getId());
        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(deviceA.getId(), deviceB.getId());
        assertNull(peer);

        verify(notificationService).notifyDevice(new NotificationBuilder()
                .senderDeviceId(deviceB.getId())
                .receiverDeviceId(deviceA.getId())
                .notificationType(NotificationType.DISSOCIATED.getValue()).build());


    }

    @Test
    void updatePhoto() throws InvalidFileType, DeviceNotFoundException, FileTooLargeException, IOException {
        Device device = new Device();
        device.setName("android_");
        deviceRepository.save(device);

        LOGGER.debug("Test with null file");
        assertThrows(IllegalArgumentException.class, () -> deviceService.updatePhoto(device.getId(), null));

        LOGGER.debug("Test with text file device ");
        MultipartFile invalidFileType = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello rapidxender!".getBytes()
        );

        assertThrows(InvalidFileType.class, () -> deviceService.updatePhoto(device.getId(), invalidFileType));

        File file = new File(getClass().getClassLoader().getResource("rapidxend.png").getFile());

        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "rapidxend.png",
                MediaType.IMAGE_PNG_VALUE,
                Files.readAllBytes(file.toPath())
        );

        LOGGER.debug("Test with null device id argument");
        assertThrows(IllegalArgumentException.class, () -> deviceService.updatePhoto(null, validFile));

        LOGGER.debug("Test with non existent device id");
        assertThrows(DeviceNotFoundException.class, () -> deviceService.updatePhoto("invalid", validFile));

        LOGGER.debug("Test with valid argument");
        deviceService.updatePhoto(device.getId(), validFile);
        assertTrue(deviceRepository.findById(device.getId()).isPresent());
        assertNotNull(deviceRepository.findById(device.getId()).get().getPhoto());
        assertEquals(validFile.getBytes().length, deviceRepository.findById(device.getId()).get().getPhoto().getData().length);


    }

    @Test
    void getPhoto() throws DeviceNotFoundException {
        LOGGER.debug("Get device photo test...");
        Device device = new Device();
        byte[] b = new byte[1024];
        new Random().nextBytes(b);
        device.setPhoto(
                new Binary(BsonBinarySubType.BINARY, b)
        );
        deviceRepository.save(device);

        LOGGER.debug("Test with null device id");
        assertThrows(IllegalArgumentException.class, () -> deviceService.getPhoto(null));

        LOGGER.debug("Test with invalid device id");
        assertThrows(DeviceNotFoundException.class, () -> deviceService.getPhoto("invalid"));

        LOGGER.debug("Test with valid device id");
        byte[] result = deviceService.getPhoto(device.getId());

        System.out.println(result.length);
        assertTrue(Arrays.equals(b, result));


    }
}