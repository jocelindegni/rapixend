package com.saankaa.rapidxend.service.device;


import com.saankaa.rapidxend.model.*;
import com.saankaa.rapidxend.repository.IDeviceRepository;
import com.saankaa.rapidxend.repository.IPeerRepository;
import com.saankaa.rapidxend.service.device.Exception.*;
import com.saankaa.rapidxend.service.notification.INotificationService;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import org.apache.tika.Tika;
import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;


@Service
public class DeviceService implements IDeviceService {


    private final IDeviceRepository deviceRepository;
    private final IPeerRepository peerRepository;
    private final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);
    private final INotificationService notificationService;


    public DeviceService(@Autowired IDeviceRepository deviceRepository, @Autowired IPeerRepository peerRepository, @Autowired INotificationService notificationService) {
        this.deviceRepository = deviceRepository;
        this.peerRepository = peerRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Device getDeviceByNameOrId(String s) throws DeviceNotFoundException {
        LOGGER.info("Get device by name");
        if (s == null) {
            LOGGER.debug("Name is null");
            throw new IllegalArgumentException("Name is null");
        }
        Device device = deviceRepository.findByName(s.toLowerCase());
        if (device == null) {

            device = deviceRepository.findById(s).orElseThrow(
                    () -> {
                        LOGGER.error("Device not found");
                        return new DeviceNotFoundException("Device not found");
                    }
            );
        }

        return device;
    }

    /**
     * Check if deviceName contains valid characters
     *
     * @param deviceName device name
     * @return true if device name is valid
     */
    private boolean deviceNameIsValid(String deviceName) {
        return Pattern.matches("^[a-z]+[0-9_]*$", deviceName);
    }

    @Override
    public boolean checkIfDeviceNameIsFree(String deviceName) throws IllegalArgumentException, InvalidDeviceNameException {
        LOGGER.info("Check if device name is valid");
        LOGGER.debug("deviceName=" + deviceName);
        if (deviceName == null || deviceName.isEmpty()) throw new IllegalArgumentException("Device name is null");

        deviceName = deviceName.toLowerCase();
        if (!this.deviceNameIsValid(deviceName)) {
            LOGGER.error("Device name is not valid");
            throw new InvalidDeviceNameException("Device name is not valid");
        }

        return deviceRepository.findByName(deviceName) == null;
    }

    @Override
    public Device register(Device device) throws IllegalArgumentException, InvalidDeviceNameException {
        LOGGER.info("Register new device");
        LOGGER.debug("" + device);
        if (device == null) {
            LOGGER.error("Device is null");
            throw new IllegalArgumentException("Device is null");
        }
        device.setName(device.getName().toLowerCase());
        checkIfDeviceNameIsFree(device.getName());

        device.setId(null);  // To avoid modification of another device
        // Generate totp (Time based one time password) secret key
        // This secret key will be used for authenticating websocket connection and REST request
        device.setSecretKey(new DefaultSecretGenerator().generate());

        return deviceRepository.save(device);
    }

    @Override
    public Device clone(String oldDeviceId, String oldDeviceSecretKey, Device newDevice) throws IllegalArgumentException, DeviceNotFoundException, BadDeviceSecretKeyException {
        LOGGER.info("Clone device");
        LOGGER.debug("oldDeviceId=" + oldDeviceId + " oldSecretKey=" + oldDeviceSecretKey + " " + newDevice);
        if (oldDeviceId == null) {
            LOGGER.error("Old device id is null");
            throw new IllegalArgumentException("Old device id must be not null");
        }
        if (oldDeviceSecretKey == null) {
            LOGGER.error("Secret key is null");
            throw new IllegalArgumentException("Old device secret key must be not null");
        }
        if (newDevice == null) {
            LOGGER.error("New device info is null");
            throw new IllegalArgumentException("newDevice object must be not null");
        }

        Optional<Device> oldDeviceOptional = deviceRepository.findById(oldDeviceId);
        if (oldDeviceOptional.isEmpty()) throw new DeviceNotFoundException("old device id is not valid");

        Device oldDevice = oldDeviceOptional.get();
        // Verify old secret key
        if (!oldDevice.getSecretKey().equals(oldDeviceSecretKey)) {
            LOGGER.error("Old secret key is not valid");
            throw new BadDeviceSecretKeyException("Old secret key is not valid");
        }
        // Update all device info by new device
        oldDevice.setSecretKey(new DefaultSecretGenerator().generate());
        oldDevice.setBrand(newDevice.getBrand());
        oldDevice.setModel(newDevice.getModel());

        return deviceRepository.save(oldDevice);
    }


    @Override
    public Peer peering(String requesterDeviceId, String applicantDeviceId) throws IllegalArgumentException, DeviceNotFoundException, PeerConflictException {
        LOGGER.info("Peering...");
        LOGGER.debug("requesterDeviceId=" + requesterDeviceId + " applicantDeviceId=" + applicantDeviceId);
        if (requesterDeviceId == null || applicantDeviceId == null) {
            LOGGER.error("One of arguments is null");
            throw new IllegalArgumentException("Device Ids must not be null");
        }

        //Verify if Ids are valid
        Optional<Device> requesterDeviceOptional = deviceRepository.findById(requesterDeviceId);
        if (requesterDeviceOptional.isEmpty()) {
            LOGGER.error("Requester device not found");
            throw new DeviceNotFoundException("Requester device not found");
        }
        Optional<Device> applicantDeviceOptional = deviceRepository.findById(applicantDeviceId);
        if (applicantDeviceOptional.isEmpty()) {
            LOGGER.error("Applicant device not found");
            throw new DeviceNotFoundException("Applicant device not found");
        }

        Device requesterDevice = requesterDeviceOptional.get();
        Device applicantDevice = applicantDeviceOptional.get();

        // Check if they are all right peered
        Peer peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(requesterDeviceId, applicantDeviceId);
        if (peer != null) {
            // Peering all right exist
            if (peer.isAccepted()) {
                LOGGER.debug("Peering all right exist");
                throw new PeerConflictException("Peering all right exist");
            } else {
                peer.setCreatedDate(new Date()); // update date
                return peerRepository.save(peer);
            }
        }
        // Check if peering exist when current requester is applicant
        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(applicantDeviceId, requesterDeviceId);
        if (peer != null) {
            if (!peer.isAccepted()) {
                peer.setAccepted(true);
                peerRepository.save(peer);
            }
            return peer;
        }

        peer = new Peer();
        peer.setRequesterDevice(requesterDevice);
        peer.setApplicantDevice(applicantDevice);
        peer.setAccepted(false);
        peerRepository.save(peer);

        LOGGER.info("Notify peer");
        Notification notification = new NotificationBuilder()
                .notificationType(NotificationType.PEERING_REQUEST.getValue())
                .senderDeviceId(requesterDeviceId)
                .receiverDeviceId(applicantDeviceId).build();
        notificationService.notifyDevice(notification);

        return peer;
    }

    @Override
    public List<Device> getPeerDevices(String deviceId) {

        if (deviceId == null) {
            LOGGER.error("Device id must not be null");
            throw new IllegalArgumentException("Device id must not be null");
        }

        final List<Device> devices = new ArrayList<>();
        for (Peer p : peerRepository.findByRequesterDevice_IdOrApplicantDevice_Id(deviceId, deviceId)) {
            if (p.isAccepted()) {
                if (p.getApplicantDevice().getId().equals(deviceId))
                    devices.add(p.getRequesterDevice());
                else
                    devices.add(p.getApplicantDevice());

            }
        }

        return devices;
    }

    @Override
    public void acceptPeering(String requesterDeviceId, String applicantDeviceId, boolean accepted) throws IllegalArgumentException, DeviceNotFoundException {
        LOGGER.debug("requesterDeviceId=" + requesterDeviceId + " applicantDeviceId=" + applicantDeviceId);
        if (requesterDeviceId == null || applicantDeviceId == null) {
            LOGGER.error("One of arguments is null");
            throw new IllegalArgumentException("Device Ids must not be null");
        }

        Peer peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(requesterDeviceId, applicantDeviceId);
        if (peer == null) {
            LOGGER.error("Requester device id or applicant device id is not valid");
            throw new DeviceNotFoundException("Requester device id or applicant device id is not valid");
        }
        if (accepted) {
            peer.setAccepted(true);
            peerRepository.save(peer);
        } else {
            peerRepository.delete(peer);
        }

        // Notify peer
        Notification notification = new NotificationBuilder()
                .notificationType(accepted ? NotificationType.PEERING_ACCEPTED.getValue()
                        : NotificationType.PEERING_DENIED.getValue())
                .senderDeviceId(applicantDeviceId).receiverDeviceId(requesterDeviceId).build();
        notificationService.notifyDevice(notification);

    }

    @Override
    public void dissociate(String deviceId, String peerDeviceId) {
        if (deviceId == null) {
            LOGGER.debug("Device id must not be null");
            throw new IllegalArgumentException("Device id must not be null");
        }

        if (peerDeviceId == null) {
            LOGGER.debug("Peer device id must be not null");
            throw new IllegalArgumentException("Peer device id must not be null");
        }

        Peer peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(deviceId, peerDeviceId);
        if (peer != null) {
            peerRepository.delete(peer);
            Notification notification = new NotificationBuilder()
                    .notificationType(NotificationType.DISSOCIATED.getValue())
                    .senderDeviceId(deviceId).receiverDeviceId(peerDeviceId).build();
            notificationService.notifyDevice(notification);
            return;
        }

        peer = peerRepository.findByRequesterDevice_IdAndApplicantDevice_Id(peerDeviceId, deviceId);
        if (peer != null) {
            peerRepository.delete(peer);
            Notification notification = new NotificationBuilder()
                    .notificationType(NotificationType.DISSOCIATED.getValue())
                    .receiverDeviceId(peerDeviceId)
                    .senderDeviceId(deviceId).build();
            notificationService.notifyDevice(notification);
        }

    }

    @Override
    public void updatePhoto(String deviceId, MultipartFile multipartFile) throws FileTooLargeException, InvalidFileType, DeviceNotFoundException, IOException {
        if (deviceId == null) {
            LOGGER.error("Device id is required");
            throw new IllegalArgumentException("Device id is required");
        }

        if (multipartFile == null) {
            LOGGER.error("File is required");
            throw new IllegalArgumentException("File is required");
        }

        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> {
            LOGGER.error("Device not found");
            return new DeviceNotFoundException("Device not found");
        });

        LOGGER.info("Checking file type");
        String mimetype = new Tika().detect(multipartFile.getBytes());
        if (!mimetype.contains("image/")) {
            LOGGER.error("Invalid file type. File type must be an image");
            throw new InvalidFileType("Invalid file type. File type must be an image");
        }

        LOGGER.info("Checking file size...");
        if (multipartFile.getBytes().length > 1024 * 1024) {
            LOGGER.error("File is too large (>1Mio)");
            throw new FileTooLargeException("File is too large (>1Mio)");
        }

        device.setPhoto(
                new Binary(BsonBinarySubType.BINARY, multipartFile.getBytes())
        );
        deviceRepository.save(device);

    }

    @Override
    public byte[] getPhoto(String deviceId) throws DeviceNotFoundException {
        if (deviceId == null) {
            LOGGER.error("Device id is required");
            throw new IllegalArgumentException("Device id is required");
        }

        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> {
            LOGGER.error("Device not found");
            return new DeviceNotFoundException("Device not found");
        });

        return device.getPhoto().getData();
    }
}
