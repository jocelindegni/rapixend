package com.saankaa.rapidxend.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.saankaa.rapidxend.model.File;
import com.saankaa.rapidxend.model.Transfer;
import com.saankaa.rapidxend.service.device.Exception.DeviceNotFoundException;
import com.saankaa.rapidxend.service.transfer.TransferService;
import com.saankaa.rapidxend.service.transfer.exception.FileCorruptedException;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import com.saankaa.rapidxend.service.transfer.exception.InvalidDataBlockException;
import com.saankaa.rapidxend.service.transfer.exception.TransferNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(@Autowired TransferService transferService) {
        this.transferService = transferService;
    }


    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public List<Transfer> getAllTransfers(@RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
                                          @RequestParam(value = "pageIndex", defaultValue = "0") int pageIndex) {

        try {
            Pageable p = Pageable.ofSize(pageSize);
            // TODO set device id
            return transferService.getTransfers("deviceId", p.withPage(pageIndex));
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException dne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, dne.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/inProgress")
    @ResponseStatus(HttpStatus.OK)
    public List<Transfer> getInProgressTransfers() {

        try {
            // TODO set device id
            return transferService.getInProgressTransfers("deviceId");
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Transfer createTransfer(@RequestBody Map<String, Object> body) {

        try {
            final ObjectMapper mapper = new ObjectMapper();
            File file = mapper.convertValue(body.get("file"), File.class);

            List<String> receiverIds = new ArrayList<>();
            for (Object o : mapper.convertValue(body.get("receiverIds"), List.class)) {
                receiverIds.add((String) o);
            }

            Transfer transfer = new Transfer();
            transfer.setFile(file);

            // TODO set sender Id
            return transferService.create("senderId", transfer, receiverIds);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (FileTooLargeException fte) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, fte.getMessage());
        } catch (DeviceNotFoundException dne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, dne.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/{transferId}")
    @ResponseStatus(HttpStatus.OK)
    public void cancelTransfer(@PathVariable("transferId") String transferId) {

        try {
            // TODO set right device id
            transferService.cancel(transferId, "deviceId");
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException | TransferNotFoundException tne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, tne.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/{transferId}/send")
    @ResponseStatus(HttpStatus.OK)
    public int sendData(@PathVariable("transferId") String transferId, @RequestParam("file") MultipartFile data) {

        try {
            return transferService.sendData(
                    transferId,
                    "deviceId", // TODO set device Id
                    data.getBytes()
            );
        } catch (IllegalArgumentException | FileCorruptedException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException | TransferNotFoundException tne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, tne.getMessage());
        } catch (FileTooLargeException idb) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, idb.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @PostMapping("/{transferId}/receive/{dataBlockNumber}")
    @ResponseStatus(HttpStatus.OK)
    public byte[] receiveData(@PathVariable("transferId") String transferId, @PathVariable("dataBlockNumber") int dataBlockNumber) {

        try {
            return transferService.receiveData(
                    transferId,
                    "deviceId",
                    dataBlockNumber
            );
        } catch (IllegalArgumentException | InvalidDataBlockException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException dne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, dne.getMessage());
        } catch (TransferNotFoundException tne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, tne.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
