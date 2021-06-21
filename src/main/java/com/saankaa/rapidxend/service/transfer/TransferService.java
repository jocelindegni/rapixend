package com.saankaa.rapidxend.service.transfer;

import com.saankaa.rapidxend.model.*;
import com.saankaa.rapidxend.repository.IDataBlockRepository;
import com.saankaa.rapidxend.repository.IDeviceRepository;
import com.saankaa.rapidxend.repository.IFileRepository;
import com.saankaa.rapidxend.repository.ITransferRepository;
import com.saankaa.rapidxend.service.device.Exception.DeviceNotFoundException;
import com.saankaa.rapidxend.service.notification.INotificationService;
import com.saankaa.rapidxend.service.transfer.exception.FileCorruptedException;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import com.saankaa.rapidxend.service.transfer.exception.InvalidDataBlockException;
import com.saankaa.rapidxend.service.transfer.exception.TransferNotFoundException;
import org.apache.commons.codec.DecoderException;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class TransferService implements ITransferService {

    private final Logger LOGGER = LoggerFactory.getLogger(TransferService.class);

    private final ITransferRepository transferRepository;
    private final IFileRepository fileRepository;
    private final IDataBlockRepository dataBlockRepository;
    private final IDeviceRepository deviceRepository;
    private final INotificationService notificationService;

    public TransferService(@Autowired ITransferRepository transferRepository, @Autowired IFileRepository fileRepository,
                           @Autowired IDataBlockRepository dataBlockRepository, @Autowired IDeviceRepository deviceRepository,
                           @Autowired INotificationService notificationService) {
        this.transferRepository = transferRepository;
        this.fileRepository = fileRepository;
        this.dataBlockRepository = dataBlockRepository;
        this.deviceRepository = deviceRepository;
        this.notificationService = notificationService;
    }


    @Override
    public List<Transfer> getTransfers(String deviceId, Pageable pageable) throws DeviceNotFoundException {
        if (deviceId == null) {
            LOGGER.debug("Device id is required");
            throw new IllegalArgumentException("Device id is required");
        }
        if (pageable == null) {
            // Return first page with 50  elements
            pageable = Pageable.ofSize(50);
        }
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> {
            LOGGER.debug("Device not found");
            return new DeviceNotFoundException("Device not found");
        });
        return transferRepository.findAllBySenderIsOrReceiversContains(device, device, pageable);
    }

    private void verifyIfTransferAttributes(Transfer transfer) {
        // Verify transfer and file required parameters
        if (transfer == null) {
            LOGGER.error("Transfer object is null");
            throw new IllegalArgumentException("Transfer object is null");
        }
        File file = transfer.getFile();
        if (file == null) {
            LOGGER.error("File object is required");
            throw new IllegalArgumentException("File object is required");
        }
        if ((file.getFilename() == null) || (file.getFilename().isEmpty())) {
            LOGGER.error("Filename is required");
            throw new IllegalArgumentException("Filename is required");
        }
        if ((file.getMD5ChecksumHex() == null) || (file.getMD5ChecksumHex().isEmpty())) {
            LOGGER.error("File checksum is required");
            throw new IllegalArgumentException("File checksum is required");
        }
        if ((file.getSize() == null)) {
            throw new IllegalArgumentException("File size is required");
        }
    }

    @Override
    public List<Transfer> getInProgressTransfers(String deviceId) {
        if (deviceId == null) {
            LOGGER.debug("Device Id must not be null");
            throw new IllegalArgumentException("Device Id must not be null");
        }
        return transferRepository.findByStateAndSender_Id(TransferState.IN_PROGRESS, deviceId);
    }

    @Override
    @Transactional
    public Transfer create(String senderDeviceId, Transfer transfer, List<String> receiverDeviceIds) throws DeviceNotFoundException, FileTooLargeException {
        if ((senderDeviceId == null) || (receiverDeviceIds == null) || (receiverDeviceIds.size() == 0)) {
            LOGGER.error("Parameters contains null values or receivers device id list is empty");
            throw new IllegalArgumentException("Parameters contains null values or receivers device id list is empty");
        }
        LOGGER.debug("Verification transfer required attribute");
        verifyIfTransferAttributes(transfer);

        Transfer newTransfer = new Transfer();
        newTransfer.setState(TransferState.IN_PROGRESS);
        File file = new File();
        file.setFilename(transfer.getFile().getFilename());
        file.setSize(transfer.getFile().getSize());
        file.setMD5ChecksumHex(transfer.getFile().getMD5ChecksumHex().toUpperCase());
        // Check if file size is not superior than max size
        if (file.getSize() > 3000) {
            LOGGER.error("File size is too large. Max size=3GiB");
            throw new FileTooLargeException("File size is too large. Max size=3GiB");
        }
        // Determine data block size
        if (file.getSize() > 300 && file.getSize() < 1000) { // >300 Mio < 1GiB
            file.setDataBlockSize(10); // 10 MB
        } else if (file.getSize() >= 1000) { // > 1GiB
            file.setDataBlockSize(50);
        }

        LOGGER.debug("Verify sender device id validity...");
        Optional<Device> optionalDevice = deviceRepository.findById(senderDeviceId);
        if (optionalDevice.isEmpty()) {
            LOGGER.error("Invalid Id of device sender");
            throw new DeviceNotFoundException("Invalid Id of device sender");
        }

        // Set file to transfer
        newTransfer.setFile(file);
        newTransfer.setSender(optionalDevice.get());

        // Set receivers device
        newTransfer.setReceivers(new ArrayList<>());
        for (String receiverDeviceId : receiverDeviceIds) {
            optionalDevice = deviceRepository.findById(receiverDeviceId);
            if (optionalDevice.isEmpty()) {
                LOGGER.error("Receiver device not found");
                LOGGER.debug("id=" + receiverDeviceId);
                throw new DeviceNotFoundException("Receiver device not found");
            }
            newTransfer.getReceivers().add(optionalDevice.get());
        }

        fileRepository.save(file);
        transferRepository.save(newTransfer);

        LOGGER.info("Notify receiver...");
        NotificationBuilder notificationBuilder = new NotificationBuilder()
                .senderDeviceId(senderDeviceId)
                .transferId(newTransfer.getId())
                .notificationType(NotificationType.TRANSFER_CREATED.getValue());
        for (String id : receiverDeviceIds) {
            notificationService.notifyDevice(notificationBuilder.receiverDeviceId(id).build());
        }
        return newTransfer;
    }

    private void deleteTransfer(Transfer transfer) {
        if (transfer != null) {
            LOGGER.debug("Delete file");
            File file = transfer.getFile();
            if (file != null) {
                LOGGER.debug("Delete all dataBlock");
                fileRepository.delete(transfer.getFile());
                List<DataBlock> dataBlocks = transfer.getFile().getDataBlocks();
                if (dataBlocks != null)
                    dataBlockRepository.deleteAll(dataBlocks);
            }
            LOGGER.debug("Delete transfer");
            transferRepository.delete(transfer);
        }
    }

    @Override
    @Transactional
    public void cancel(String transferId, String deviceId) throws DeviceNotFoundException, TransferNotFoundException {
        LOGGER.debug("Check parameters validity");
        if ((transferId == null) || (deviceId == null)) {
            LOGGER.debug("Parameters contains null value");
            throw new IllegalArgumentException("Parameters contains null value");
        }
        LOGGER.debug("Check if it's the sender");
        Transfer transfer = transferRepository.findByIdAndSender_Id(transferId, deviceId);
        if (transfer != null) {
            LOGGER.info("Delete transfer");
            this.deleteTransfer(transfer);

            LOGGER.debug("Notify all receivers that transfer has been deleted");
            List<Device> receivers = transfer.getReceivers();
            for (Device r : receivers) {
                notificationService.notifyDevice(new NotificationBuilder()
                        .transferId(transfer.getId())
                        .senderDeviceId(deviceId)
                        .receiverDeviceId(r.getId())
                        .notificationType(NotificationType.TRANSFER_CANCELLED.getValue()).build());
            }
            return; // To avoid execution of code below
        }

        LOGGER.debug("Check it's a receiver. So if its not the sender");
        Optional<Device> optionalDevice = deviceRepository.findById(deviceId);
        if (optionalDevice.isEmpty()) {
            LOGGER.debug("Device not found");
            throw new DeviceNotFoundException("Device not found");
        }

        Device device = optionalDevice.get();
        transfer = transferRepository.findByIdAndReceiversContains(transferId, device);
        if (transfer != null) {

            LOGGER.debug("Delete current device from all receivers device of current transfer");
            transfer.setReceivers(
                    transfer.getReceivers().stream().filter(d -> !d.getId().equals(deviceId)).collect(Collectors.toUnmodifiableList())
            );

            LOGGER.debug("Notify sender that current receiver has cancelled transfer");
            Notification notification = new NotificationBuilder()
                    .transferId(transferId)
                    .senderDeviceId(device.getId())
                    .receiverDeviceId(transfer.getSender().getId())
                    .notificationType(NotificationType.TRANSFER_CANCELLED.getValue()).build();
            LOGGER.info("" + notification);
            notificationService.notifyDevice(notification);


            LOGGER.debug("Check if there is a receiver");
            if (transfer.getReceivers().size() == 0) {
                LOGGER.debug("There is no receiver . Cancel current transfer...");
                this.deleteTransfer(transfer);
                return;
            }

            transferRepository.save(transfer);
        } else {
            LOGGER.debug("Transfer not found");
            throw new TransferNotFoundException("Transfer not found");
        }
    }

    private String MD5Hex(List<DataBlock> dataBlocks) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        for (DataBlock dataBlock : dataBlocks)
            md.update(dataBlock.getData());

        return DatatypeConverter.printHexBinary(md.digest());
    }

    @Override
    @Transactional
    public int sendData(String transferId, String senderDeviceId, byte[] data) throws TransferNotFoundException, InvalidDataBlockException, NoSuchAlgorithmException, FileTooLargeException, FileCorruptedException, DeviceNotFoundException, DecoderException {
        if ((transferId == null) || (senderDeviceId == null) || (data == null)) {
            LOGGER.error("Parameters contains null values");
            throw new IllegalArgumentException("Parameters contains null values");
        }

        if (data.length == 0) {
            LOGGER.error("Data block is 0");
            throw new IllegalArgumentException("Data must not be empty");
        }

        Transfer transfer = transferRepository.findById(transferId).orElseThrow(() -> {
            LOGGER.error("Transfer not found");
            return new TransferNotFoundException("Transfer not found");
        });

        LOGGER.info("Check if its the sender");
        if (!transfer.getSender().getId().equals(senderDeviceId)) {
            LOGGER.error("Invalid sender id");
            throw new DeviceNotFoundException("Invalid sender id");
        }

        LOGGER.info("Check transfer status");
        if (transfer.getState() == TransferState.FINISHED) {
            LOGGER.error("Transfer with FINISHED state cannot be modified");
            throw new TransferNotFoundException("Transfer with FINISHED state can't be modified");
        }

        // Check if its the first block to determine mimetype
        File file = transfer.getFile();
        if (file.getDataBlocks() == null) {
            LOGGER.info("First data block");
            LOGGER.info("Get mimetype");
            file.setMimetype(new Tika().detect(data)); // Apache tika for auto-detecting of stream mimetype
            file.setDataBlocks(new ArrayList<>());
        }

        LOGGER.info("Checking data block size...");
        if (file.getDataBlockSize() < data.length / 1024 / 1024) { // Size is in MiB
            LOGGER.error("Data block size is not valid");
            LOGGER.debug("Max data block size {} MiB, data block received {} MiB", file.getDataBlockSize(), data.length * 1024);
            throw new FileTooLargeException("Data received is too large that data block size.");
        }

        LOGGER.info("Create data block and add to file");
        int nextBlockNumber = file.getLastDataBlockNumber() + 1;
        DataBlock dataBlock = new DataBlock();
        dataBlock.setData(data);
        file.setLastDataBlockNumber(nextBlockNumber);
        dataBlock.setNumber(nextBlockNumber);
        file.getDataBlocks().add(dataBlock);

        // Check if its the last block for computing checksum to verify file integrity
        if (nextBlockNumber == Math.round(file.getSize() / file.getDataBlockSize()) - 1) {

            LOGGER.debug("File checksum from user " + file.getMD5ChecksumHex());
            LOGGER.info("Compute checksum to verify file integrity...");
            String computedChecksum = this.MD5Hex(file.getDataBlocks());
            LOGGER.debug("Computed checksum " + computedChecksum);

            if (!computedChecksum.equals(file.getMD5ChecksumHex())) {
                LOGGER.error("The checksum is invalid. Canceling transfer...");
                this.cancel(transfer.getId(), senderDeviceId);
                throw new FileCorruptedException("The checksum is invalid");
            }

            LOGGER.info("Set transfer state to FINISHED state");
            transfer.setState(TransferState.FINISHED);
        }

        LOGGER.info("Update transfer info");
        transfer.setFile(file);
        dataBlockRepository.save(dataBlock);
        fileRepository.save(file);
        transferRepository.save(transfer);

        LOGGER.info("Notify all receivers that a new data block is available");
        List<Device> receivers = transfer.getReceivers();
        NotificationBuilder notificationBuilder = new NotificationBuilder()
                .senderDeviceId(senderDeviceId)
                .dataBlockNumber(nextBlockNumber)
                .transferId(transferId)
                .notificationType(NotificationType.TRANSFER_DATA_AVAILABLE.getValue());

        for (Device r : receivers) {
            notificationService.notifyDevice(notificationBuilder.receiverDeviceId(r.getId()).build());
        }

        return transfer.getFile().getLastDataBlockNumber();
    }

    @Override
    public byte[] receiveData(final String transferId, final String receiverDeviceId, final Integer dataBlockNumber) throws TransferNotFoundException, DeviceNotFoundException, InvalidDataBlockException {
        if (transferId == null) {
            LOGGER.error("Transfer id must not be null");
            throw new IllegalArgumentException("Transfer id is null");
        }

        if (receiverDeviceId == null) {
            LOGGER.error("Receiver id must not be null");
            throw new IllegalArgumentException("Parameters can't be null");
        }

        if (dataBlockNumber == null) {
            LOGGER.error("Parameters contains null");
            throw new IllegalArgumentException("Parameters can't be null");
        }

        LOGGER.debug("Get device");
        Optional<Device> optionalDevice = deviceRepository.findById(receiverDeviceId);
        if (optionalDevice.isEmpty()) {
            LOGGER.debug("Invalid device Id");
            throw new DeviceNotFoundException("Invalid device Id");
        }
        Device device = optionalDevice.get();

        // Get transfer
        Transfer transfer = transferRepository.findByIdAndReceiversContains(transferId, device);
        if (transfer == null) {
            LOGGER.debug("Transfer not found");
            throw new TransferNotFoundException("Transfer not found");
        }

        // Check if data block exist
        Optional<DataBlock> optionalDataBlock = Optional.empty();
        try {
            optionalDataBlock = transfer.getFile().getDataBlocks().stream().filter(db -> db.getNumber().equals(dataBlockNumber)).findFirst();
        } catch (NullPointerException ignored) {
        }

        if (optionalDataBlock.isEmpty()) {
            LOGGER.error("Invalid dataBlock number");
            throw new InvalidDataBlockException("Invalid dataBlock number");
        }

        // If it's the last data block -> notify sender
        DataBlock dataBlock = optionalDataBlock.get();
        File file = transfer.getFile();

        if (dataBlock.getNumber() == Math.round(file.getSize() / file.getDataBlockSize()) - 1) {

            LOGGER.debug("Receiver has finished downloading");
            LOGGER.debug("Notify sender...");

            Notification notification = new NotificationBuilder()
                    .transferId(transferId)
                    .senderDeviceId(receiverDeviceId)
                    .receiverDeviceId(transfer.getSender().getId())
                    .notificationType(NotificationType.TRANSFER_FINISHED.getValue()).build();

            notificationService.notifyDevice(notification);
        }

        return dataBlock.getData();
    }
}
