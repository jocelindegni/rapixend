package com.saankaa.rapidxend.service.device;

import com.saankaa.rapidxend.model.Device;
import com.saankaa.rapidxend.model.Peer;
import com.saankaa.rapidxend.service.device.Exception.*;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface IDeviceService {

    /**
     * Get device by name
     *
     * @param name Device name
     * @return Device
     */
    Device getDeviceByNameOrId(String name) throws DeviceNotFoundException;

    /**
     * @param deviceName name to check
     * @return true if deviceName exist in database
     * @throws IllegalArgumentException, InvalidDeviceNameException
     */
    boolean checkIfDeviceNameIsFree(String deviceName) throws IllegalArgumentException, InvalidDeviceNameException;

    /**
     * Register device
     *
     * @param device device info
     * @return Device created
     * @throws IllegalArgumentException, InvalidDeviceNameException
     */
    Device register(Device device) throws IllegalArgumentException, InvalidDeviceNameException;

    /**
     * Allow user ot clone all information from old device to new device
     * Indeed, this method generate new secret key and send it to the new device
     *
     * @param oldDeviceId        Old device id
     * @param oldDeviceSecretKey old device secret key
     * @param newDevice          newDevice information
     * @return device updated
     */
    Device clone(String oldDeviceId, String oldDeviceSecretKey, Device newDevice) throws IllegalArgumentException, DeviceNotFoundException, BadDeviceSecretKeyException;


    /**
     * Get peer devices
     *
     * @param deviceId Device which want get his peer
     * @return
     */
    List<Device> getPeerDevices(String deviceId);

    /**
     * Send peering request
     * Allow device A to peer with another device B
     *
     * @param requesterDeviceId Device id of requester. Device which want to peer
     * @param applicantDeviceId Applicant device id
     * @return Peer information
     */
    Peer peering(String requesterDeviceId, String applicantDeviceId) throws IllegalArgumentException, DeviceNotFoundException, PeerConflictException;


    /**
     * Accept peering
     *
     * @param requesterDeviceId Device id of applicant peering
     * @param applicantDeviceId Device id of applicant peering
     * @param accepted          True if applicant accept peering
     */
    void acceptPeering(String requesterDeviceId, String applicantDeviceId, boolean accepted) throws IllegalArgumentException, DeviceNotFoundException;

    /**
     * @param deviceId     Device id
     * @param peerDeviceId Peer device id
     */
    void dissociate(String deviceId, String peerDeviceId);


    /**
     * Update device photo
     *
     * @param deviceId Device id
     * @param file     Image file
     * @throws FileTooLargeException When image size is > than 1MB
     * @throws InvalidFileType       When file mimetype is not an image
     */
    void updatePhoto(String deviceId, MultipartFile file) throws FileTooLargeException, InvalidFileType, DeviceNotFoundException, IOException;

    /**
     * Return device photo
     *
     * @param deviceId Device id
     * @return Image file
     */
    byte[] getPhoto(String deviceId) throws DeviceNotFoundException;


}
