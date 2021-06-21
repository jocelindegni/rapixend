package com.saankaa.rapidxend.service.transfer;

import com.saankaa.rapidxend.model.*;
import com.saankaa.rapidxend.repository.IDataBlockRepository;
import com.saankaa.rapidxend.repository.IDeviceRepository;
import com.saankaa.rapidxend.repository.IFileRepository;
import com.saankaa.rapidxend.repository.ITransferRepository;
import com.saankaa.rapidxend.service.device.Exception.DeviceNotFoundException;
import com.saankaa.rapidxend.service.notification.NotificationService;
import com.saankaa.rapidxend.service.transfer.exception.FileCorruptedException;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import com.saankaa.rapidxend.service.transfer.exception.InvalidDataBlockException;
import com.saankaa.rapidxend.service.transfer.exception.TransferNotFoundException;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ITransferServiceTest {

    private final Logger LOGGER = LoggerFactory.getLogger(ITransferServiceTest.class);
    @MockBean
    NotificationService notificationService;
    @Autowired
    private ITransferRepository transferRepository;
    @Autowired
    private IFileRepository fileRepository;
    @Autowired
    private IDataBlockRepository dataBlockRepository;
    @Autowired
    private IDeviceRepository deviceRepository;
    @Autowired
    private ITransferService transferService;

    @BeforeEach
    void setUp() {
        dataBlockRepository.deleteAll();
        fileRepository.deleteAll();
        transferRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void getInProgressTransfers() {
        LOGGER.debug("Test getInProgressTransfers");
        LOGGER.debug("Test with null device id");
        assertThrows(IllegalArgumentException.class, () -> transferService.getInProgressTransfers(null));

        Device device = new Device();
        deviceRepository.save(device);

        Transfer transfer1 = new Transfer();
        transfer1.setState(TransferState.IN_PROGRESS);
        transfer1.setSender(device);

        Transfer transfer2 = new Transfer();
        transfer2.setState(TransferState.IN_PROGRESS);
        transfer2.setSender(device);

        Transfer transfer3 = new Transfer();
        transfer3.setState(TransferState.FINISHED);
        transfer1.setSender(device);

        transferRepository.saveAll(new ArrayList<>() {
            {
                add(transfer1);
                add(transfer2);
                add(transfer3);
            }
        });

        LOGGER.debug("Test with invalid device id");
        assertEquals(0, transferService.getInProgressTransfers("dfkjdfd").size());

        LOGGER.debug("Test with valid device id");
        List<Transfer> list = transferService.getInProgressTransfers(device.getId());
        assertTrue(list.stream().anyMatch(transfer -> transfer.getId().equals(transfer1.getId())));
        assertTrue(list.stream().anyMatch(transfer -> transfer.getId().equals(transfer2.getId())));
        assertFalse(list.stream().anyMatch(transfer -> transfer.getId().equals(transfer3.getId())));
    }

    @Test
    void create() throws FileTooLargeException, DeviceNotFoundException {
        // init
        Device sender = new Device(), receiver1 = new Device(), receiver2 = new Device();
        List<Device> receivers = new ArrayList<>() {
            {
                add(sender);
                add(receiver1);
                add(receiver2);
            }
        };
        deviceRepository.saveAll(receivers);
        List<String> receiversId = receivers.stream().map(Device::getId).collect(Collectors.toList());
        Transfer transfer = new Transfer();

        // Test transfer creating with required attributes set to null
        LOGGER.debug("Test with sender device id set to null");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(null, transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with transfer set to null");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), null, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with receivers device id list set to null");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), transfer, null));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with empty receivers device id list");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), transfer, new ArrayList<>()));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with file set to null");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with all file attribute set to null");
        File file = new File();
        transfer.setFile(file);
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with all file attributes set to null and empty filename");
        transfer.getFile().setFilename("");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());


        LOGGER.debug("Test with not empty filename");
        transfer.getFile().setFilename("rapidxend.png");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with empty md5checksum");
        transfer.getFile().setMD5ChecksumHex("");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with not empty md5checksum");
        transfer.getFile().setMD5ChecksumHex("HA5678BA5678");
        assertThrows(IllegalArgumentException.class, () -> transferService.create(sender.getId(), transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());


        LOGGER.debug("Test with size over max size");
        transfer.getFile().setSize(3000.0001); // > to 3 GB
        assertThrows(FileTooLargeException.class, () -> transferService.create(sender.getId(), transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with valid size but not valid sender device id");
        transfer.getFile().setSize(178.678); // Size > 178.678 -> data block size is 5 MB (The default value)
        assertThrows(DeviceNotFoundException.class, () -> transferService.create("invalid-sender-id", transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with valid sender but with one invalid receiver id in receivers list");
        receiversId.add("invalid");
        assertThrows(DeviceNotFoundException.class, () -> transferService.create(sender.getId(), transfer, receiversId));
        assertEquals(0, transferRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());

        LOGGER.debug("Test with valid parameters");
        receiversId.remove("invalid");
        Transfer transferReturned = transferService.create(sender.getId(), transfer, receiversId);

        Optional<Transfer> transferDbOpt = transferRepository.findById(transferReturned.getId());
        Optional<File> fileDbOpt = fileRepository.findById(transferReturned.getFile().getId());
        assertTrue(transferDbOpt.isPresent());
        assertTrue(fileDbOpt.isPresent());

        File fileDb = fileDbOpt.get();
        assertEquals("rapidxend.png", fileDb.getFilename());
        assertNull(fileDb.getMimetype());
        assertEquals(178.678, fileDb.getSize());
        assertNotNull(fileDb.getMD5ChecksumHex());
        assertEquals(5, fileDb.getDataBlockSize());
        assertNull(fileDb.getDataBlocks());
        assertEquals(-1, fileDb.getLastDataBlockNumber());
        assertNotNull(file.getCreatedDate());

        Transfer transferDb = transferDbOpt.get();
        assertEquals(fileDb.getId(), transferDb.getFile().getId());
        assertEquals(sender.getId(), transferDb.getSender().getId());
        assertEquals(TransferState.IN_PROGRESS, transferDb.getState());
        assertNotNull(transferDb.getCreatedDate());
        assertTrue(transferDb.getReceivers().stream().anyMatch(device -> device.getId().equals(receiver1.getId())));
        assertTrue(transferDb.getReceivers().stream().anyMatch(device -> device.getId().equals(receiver1.getId())));
        assertTrue(transferDb.getReceivers().stream().anyMatch(device -> device.getId().equals(receiver1.getId())));

        LOGGER.debug("Change file size(higher to 300MB");
        fileRepository.deleteAll();
        transferRepository.deleteAll();
        transfer.setId(null);
        transfer.getFile().setId(null);
        transfer.getFile().setSize(300.56);
        transferDb = transferService.create(sender.getId(), transfer, receiversId);
        assertEquals(10, transferDb.getFile().getDataBlockSize());

        LOGGER.debug("Change file size(higher to 1GB");
        fileRepository.deleteAll();
        transferRepository.deleteAll();
        transfer.setId(null);
        File file1GB = new File();
        file1GB.setFilename("rapidxend.png");
        file1GB.setMD5ChecksumHex("H4567FGH567FGH");
        file1GB.setSize(1000.10);
        transfer.setFile(file1GB);
        transferDb = transferService.create(sender.getId(), transfer, receiversId);
        assertEquals(50, transferDb.getFile().getDataBlockSize());

    }

    @Test
    void cancel() throws TransferNotFoundException, DeviceNotFoundException {
        LOGGER.debug("Test cancel method for transfer service...");

        LOGGER.debug("Create transfer with with 2 receivers");
        Device sender = new Device(), receiver1 = new Device(), receiver2 = new Device();
        List<Device> receivers = new ArrayList<>() {{
            add(sender);
            add(receiver1);
            add(receiver2);
        }};

        // Save receivers
        deviceRepository.saveAll(receivers);

        // Create transfer
        Transfer transfer = new Transfer();
        transfer.setSender(sender);
        File file = new File();
        file.setDataBlocks(new ArrayList<>() {{
            add(dataBlockRepository.save(new DataBlock()));
        }});
        fileRepository.save(file);
        transfer.setFile(file);
        transfer.setReceivers(new ArrayList<>() {{
            add(receiver1);
            add(receiver2);
        }});
        transferRepository.save(transfer);

        LOGGER.debug("Test with null transfer id");
        assertThrows(IllegalArgumentException.class, () -> transferService.cancel(null, sender.getId()));

        LOGGER.debug("Test with null device id");
        assertThrows(IllegalArgumentException.class, () -> transferService.cancel(transfer.getId(), null));

        LOGGER.debug("Test with invalid transfer id");
        assertThrows(TransferNotFoundException.class, () -> transferService.cancel("invalid-id", sender.getId()));

        LOGGER.debug("Test with null transfer id");
        assertThrows(DeviceNotFoundException.class, () -> transferService.cancel(transfer.getId(), "invalid-id"));

        LOGGER.debug("Cancel transfer by first receiver");
        // Mock notification service
        doNothing().when(notificationService).notifyDevice(isA(Notification.class));
        transferService.cancel(transfer.getId(), receiver1.getId());
        assertTrue(transferRepository.findById(transfer.getId()).isPresent());
        assertEquals(1, transferRepository.findById(transfer.getId()).get().getReceivers().size());

        LOGGER.debug("Assert that notification service is called with right argument");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .transferId(transfer.getId())
                .senderDeviceId(receiver1.getId())
                .receiverDeviceId(sender.getId())
                .notificationType(NotificationType.TRANSFER_CANCELLED.getValue()).build());

        LOGGER.debug("Cancel transfer by second receiver. This operation must delete transfer because there are no receivers");
        transferService.cancel(transfer.getId(), receiver2.getId());
        assertFalse(transferRepository.findById(transfer.getId()).isPresent());
        assertEquals(0, fileRepository.findAll().size());
        assertEquals(0, dataBlockRepository.findAll().size());

        LOGGER.debug("Assert that notification service is called with right argument");

        verify(notificationService, times(2)).notifyDevice(new NotificationBuilder()
                .transferId(transfer.getId())
                .senderDeviceId(receiver2.getId())
                .receiverDeviceId(sender.getId())
                .notificationType(NotificationType.TRANSFER_CANCELLED.getValue()).build());

        LOGGER.debug("Re-create same transfer");
        transfer.setId(null);
        file.setId(null);
        file.setDataBlocks(new ArrayList<>() {{
            add(dataBlockRepository.save(new DataBlock()));
        }});
        fileRepository.save(file);
        transferRepository.save(transfer);

        LOGGER.debug("Cancel transfer by sender. Transfer must be deleted");
        transferService.cancel(transfer.getId(), sender.getId());
        assertEquals(0, fileRepository.findAll().size());
        assertEquals(0, fileRepository.findAll().size());
        assertEquals(0, dataBlockRepository.findAll().size());

        LOGGER.debug("Assert that notification service is called for notified all receivers");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .transferId(transfer.getId())
                .senderDeviceId(sender.getId())
                .receiverDeviceId(receiver1.getId())
                .notificationType(NotificationType.TRANSFER_CANCELLED.getValue()).build());

        verify(notificationService).notifyDevice(new NotificationBuilder()
                .transferId(transfer.getId())
                .senderDeviceId(sender.getId())
                .receiverDeviceId(receiver2.getId())
                .notificationType(NotificationType.TRANSFER_CANCELLED.getValue()).build());

        LOGGER.debug("End test for cancel method");
    }

    @Test
    void sendData() throws NoSuchAlgorithmException, TransferNotFoundException, DeviceNotFoundException, InvalidDataBlockException, FileTooLargeException, FileCorruptedException, DecoderException {
        LOGGER.debug("Test send data method of transfer service...");
        LOGGER.debug("Create a transfer with a 10MB file and data block size set to 5MB. So we have to send to data block");
        Device sender = new Device();
        deviceRepository.save(sender);
        Transfer transfer = new Transfer();
        transfer.setSender(sender);
        Device receiver = new Device();
        transfer.setReceivers(
                deviceRepository.saveAll(new ArrayList<>() {{
                    add(receiver);
                }})
        );
        transfer.setState(TransferState.IN_PROGRESS);
        File file = new File();
        file.setFilename("rapidXend.png");
        file.setSize(10.0);
        file.setDataBlockSize(5);
        LOGGER.debug("Set checksum");

        byte[] firstByte = new byte[5 * 1024 * 1024];
        new Random().nextBytes(firstByte); // Fill bytes
        byte[] secondByte = new byte[5 * 1024 * 1024];
        new Random().nextBytes(secondByte);
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(firstByte, 0, firstByte.length);
        md.update(secondByte, 0, secondByte.length);
        file.setMD5ChecksumHex(DatatypeConverter.printHexBinary(md.digest()));

        fileRepository.save(file);
        transfer.setFile(file);
        transferRepository.save(transfer);

        LOGGER.debug("Test send data with null transfer id");
        assertThrows(IllegalArgumentException.class, () -> transferService.sendData(null, sender.getId(), firstByte));

        LOGGER.debug("Test send data with non existent transfer id");
        assertThrows(TransferNotFoundException.class, () -> transferService.sendData("invalid", sender.getId(), firstByte));

        LOGGER.debug("Test send data with null sender id");
        assertThrows(IllegalArgumentException.class, () -> transferService.sendData(transfer.getId(), null, firstByte));

        LOGGER.debug("Test send data with non existent sender id");
        assertThrows(DeviceNotFoundException.class, () -> transferService.sendData(transfer.getId(), "invalid", firstByte));

        LOGGER.debug("Test send data with null data");
        assertThrows(IllegalArgumentException.class, () -> transferService.sendData(transfer.getId(), sender.getId(), null));

        LOGGER.debug("Test send data with empty data");
        assertThrows(IllegalArgumentException.class, () -> transferService.sendData(transfer.getId(), sender.getId(), new byte[0]));

        // Mock notification
        doNothing().when(notificationService).notifyDevice(isA(Notification.class));

        LOGGER.debug("Send first data block");
        transferService.sendData(transfer.getId(), sender.getId(), firstByte);
        LOGGER.debug("Check if receiver has been notified");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .transferId(transfer.getId())
                .senderDeviceId(sender.getId())
                .receiverDeviceId(receiver.getId())
                .dataBlockNumber(0)
                .notificationType(NotificationType.TRANSFER_DATA_AVAILABLE.getValue()).build());

        assertEquals(1, dataBlockRepository.findAll().size());
        assertTrue(fileRepository.findById(file.getId()).isPresent());
        assertEquals(0, fileRepository.findById(file.getId()).get().getLastDataBlockNumber());
        assertEquals("application/octet-stream", fileRepository.findById(file.getId()).get().getMimetype());
        assertTrue(transferRepository.findById(transfer.getId()).isPresent());
        assertEquals(TransferState.IN_PROGRESS, transferRepository.findById(transfer.getId()).get().getState());

        LOGGER.debug("Send invalid data block. His size is higher than the max data block size");
        byte[] bigData = new byte[5 * 1024 * 2048];
        new Random().nextBytes(bigData);
        assertThrows(FileTooLargeException.class, () -> transferService.sendData(transfer.getId(), sender.getId(), bigData));

        LOGGER.debug("Send last data block");
        transferService.sendData(transfer.getId(), sender.getId(), secondByte);
        LOGGER.debug("Check if receiver has been notified");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .transferId(transfer.getId())
                .dataBlockNumber(1)
                .senderDeviceId(sender.getId())
                .receiverDeviceId(receiver.getId())
                .notificationType(NotificationType.TRANSFER_DATA_AVAILABLE.getValue()).build());

        assertEquals(2, dataBlockRepository.findAll().size());
        assertTrue(fileRepository.findById(file.getId()).isPresent());
        assertEquals(1, fileRepository.findById(file.getId()).get().getLastDataBlockNumber());
        assertEquals("application/octet-stream", fileRepository.findById(file.getId()).get().getMimetype());
        assertTrue(transferRepository.findById(transfer.getId()).isPresent());
        assertEquals(TransferState.FINISHED, transferRepository.findById(transfer.getId()).get().getState());

        LOGGER.debug("Send another data block. Excepted TransferNotFoundException ");
        byte[] anotherData = new byte[5 * 1024 * 1024];
        new Random().nextBytes(anotherData);
        assertThrows(TransferNotFoundException.class, () -> transferService.sendData(transfer.getId(), sender.getId(), anotherData));

        LOGGER.debug("Try to throw FileCorruptedException...");
        dataBlockRepository.deleteAll();
        fileRepository.deleteAll();
        transferRepository.deleteAll();
        file.setDataBlocks(null);
        fileRepository.save(file);
        transferRepository.save(transfer);
        transferService.sendData(transfer.getId(), sender.getId(), firstByte);
        byte[] invalidSecondByte = new byte[5 * 1024 * 1024];
        new Random().nextBytes(invalidSecondByte);
        assertThrows(FileCorruptedException.class, () -> transferService.sendData(transfer.getId(), sender.getId(), invalidSecondByte));

        LOGGER.debug("End test of send data method");

    }

    @Test
    void receiveData() throws TransferNotFoundException, InvalidDataBlockException, DeviceNotFoundException {
        LOGGER.debug("Test of receive data method of transfer service...");

        LOGGER.debug("Create transfer which contains file with 2 data blocks");
        DataBlock db1 = new DataBlock(), db2 = new DataBlock();
        db1.setNumber(0);
        db1.setData("rapid".getBytes());
        db2.setNumber(1);
        db2.setData("xend".getBytes());
        List<DataBlock> dataBlocks = new ArrayList<>() {{
            add(db1);
            add(db2);
        }};
        dataBlockRepository.saveAll(dataBlocks);

        File file = new File();
        file.setDataBlocks(dataBlocks);

        file.setSize(10.0);
        file.setDataBlockSize(5);
        fileRepository.save(file);

        Device receiver = new Device(), sender = new Device();
        deviceRepository.save(receiver);
        deviceRepository.save(sender);

        Transfer transfer = new Transfer();
        transfer.setFile(file);
        transfer.setSender(sender);
        transfer.setReceivers(new ArrayList<>() {{
            add(receiver);
        }});
        transferRepository.save(transfer);

        LOGGER.debug("Test with null transfer id");
        assertThrows(IllegalArgumentException.class, () -> transferService.receiveData(null, receiver.getId(), 0));

        LOGGER.debug("Test with null receiver id");
        assertThrows(IllegalArgumentException.class, () -> transferService.receiveData(transfer.getId(), null, 0));

        LOGGER.debug("Test with null data block number");
        assertThrows(IllegalArgumentException.class, () -> transferService.receiveData(transfer.getId(), receiver.getId(), null));

        LOGGER.debug("Test with invalid transfer id");
        assertThrows(TransferNotFoundException.class, () -> transferService.receiveData("invalid-id", receiver.getId(), 0));

        LOGGER.debug("Test with invalid receiver id");
        assertThrows(DeviceNotFoundException.class, () -> transferService.receiveData(transfer.getId(), "invalid-id", 0));

        LOGGER.debug("Test with invalid data block number");
        assertThrows(InvalidDataBlockException.class, () -> transferService.receiveData(transfer.getId(), receiver.getId(), 2));


        LOGGER.debug("Get first data block");
        assertEquals(2, dataBlockRepository.findAll().size());
        assertEquals(2, fileRepository.findById(file.getId()).get().getDataBlocks().size());
        assertEquals("rapid",
                new String(transferService.receiveData(transfer.getId(), receiver.getId(), 0)));

        LOGGER.debug("Get second data block(The last). Note that sender must be notified");
        // Mock notification service
        doNothing().when(notificationService).notifyDevice(isA(Notification.class));
        assertEquals("xend",
                new String(transferService.receiveData(transfer.getId(), receiver.getId(), 1)));

        LOGGER.debug("Assert that sender has been notified");
        verify(notificationService).notifyDevice(new NotificationBuilder()
                .transferId(transfer.getId())
                .senderDeviceId(receiver.getId())
                .receiverDeviceId(sender.getId())
                .notificationType(NotificationType.TRANSFER_FINISHED.getValue()).build());
        LOGGER.debug("End test of receiver method");

    }

    @Test
    void getTransfers() throws DeviceNotFoundException {
        LOGGER.debug("Create one device...");
        Device device = deviceRepository.save(new Device());
        LOGGER.debug("Create 50 transfers for current device and 50 for anonymous device...");
        for (int i = 0; i < 50; i++) {
            Transfer transfer = new Transfer();
            transfer.setSender(device);
            transferRepository.save(transfer);

            Transfer transfer1 = new Transfer();
            transfer1.setSender(deviceRepository.save(new Device()));
            transferRepository.save(transfer1);
        }
        assertEquals(100, transferRepository.findAll().size());


        LOGGER.debug("Test with null device id");
        final Pageable pageable = Pageable.ofSize(20);
        pageable.withPage(0);
        assertThrows(IllegalArgumentException.class, () -> transferService.getTransfers(null, pageable.withPage(1)));

        LOGGER.debug("Test with invalid device id");
        assertThrows(DeviceNotFoundException.class, () -> transferService.getTransfers("invalid", pageable.withPage(1)));

        LOGGER.debug("Test with null pageable");
        List<Transfer> list = transferService.getTransfers(device.getId(), null);
        assertEquals(50, list.size());

        LOGGER.debug("Test with valid pageable size =20");

        LOGGER.debug("Get first page...");
        assertEquals(20, transferService.getTransfers(device.getId(), pageable).size());

        LOGGER.debug("Get SECOND page...");
        assertEquals(20, transferService.getTransfers(device.getId(), pageable.withPage(1)).size());

        LOGGER.debug("Get THIRD page...");
        assertEquals(10, transferService.getTransfers(device.getId(), pageable.withPage(2)).size());

        LOGGER.debug("Try to get fourth page...");
        assertEquals(0, transferService.getTransfers(device.getId(), pageable.withPage(3)).size());

    }
}