package com.saankaa.rapidxend.service.transfer;

import com.saankaa.rapidxend.model.Transfer;
import com.saankaa.rapidxend.service.device.Exception.DeviceNotFoundException;
import com.saankaa.rapidxend.service.transfer.exception.FileCorruptedException;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import com.saankaa.rapidxend.service.transfer.exception.InvalidDataBlockException;
import com.saankaa.rapidxend.service.transfer.exception.TransferNotFoundException;
import org.apache.commons.codec.DecoderException;
import org.springframework.data.domain.Pageable;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface ITransferService {

    /**
     * Get Transfer list for specifi device.
     *
     * @param deviceId Device id
     * @param pageable Pageable object to
     * @return List of transfer. List transfer is sorted (DESC) by createdDate attribute
     */
    List<Transfer> getTransfers(String deviceId, Pageable pageable) throws DeviceNotFoundException;

    /**
     * In progress transfer for current device
     *
     * @param deviceId Device Id
     * @return List of transfer(in progress state)
     */
    List<Transfer> getInProgressTransfers(String deviceId);

    /**
     * Start
     *
     * @param senderDeviceId    Sender device id
     * @param transfer          Transfer information. Information about file, sender and receivers.
     * @param receiverDeviceIds Receivers device id
     * @return transfer created
     */
    Transfer create(String senderDeviceId, Transfer transfer, List<String> receiverDeviceIds) throws DeviceNotFoundException, FileTooLargeException;

    /**
     * Stop transfer
     *
     * @param transferId Transfer id
     * @param deviceId   sender  or receiver device id
     */
    void cancel(String transferId, String deviceId) throws DeviceNotFoundException, TransferNotFoundException;

    /**
     * Sending data
     *
     * @param transferId     Transfer id
     * @param senderDeviceId Sender device id
     * @param data           Data to send
     * @return Last data block number
     */
    int sendData(String transferId, String senderDeviceId, byte[] data) throws TransferNotFoundException, InvalidDataBlockException, NoSuchAlgorithmException, FileTooLargeException, FileCorruptedException, DeviceNotFoundException, DecoderException;

    /**
     * Receive data
     *
     * @param receiverDeviceId Receiver device id
     * @param dataBlockNumber  data block number
     * @return data
     */
    byte[] receiveData(String transferId, String receiverDeviceId, Integer dataBlockNumber) throws TransferNotFoundException, DeviceNotFoundException, InvalidDataBlockException;
}
