package com.saankaa.rapidxend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saankaa.rapidxend.model.Device;
import com.saankaa.rapidxend.model.Peer;
import com.saankaa.rapidxend.service.Authentication.IAuthenticationService;
import com.saankaa.rapidxend.service.device.Exception.*;
import com.saankaa.rapidxend.service.device.IDeviceService;
import com.saankaa.rapidxend.service.transfer.exception.FileTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final Logger LOGGER = LoggerFactory.getLogger(DeviceController.class);

    private final IDeviceService deviceService;

    private final IAuthenticationService authenticationService;

    public DeviceController(@Autowired IDeviceService deviceService, @Autowired IAuthenticationService authenticationService) {
        this.deviceService = deviceService;
        this.authenticationService = authenticationService;
    }


    @GetMapping("/{idOrName}")
    @ResponseStatus(HttpStatus.OK)
    public Device getDeviceByIdOrName(@PathVariable("idOrName") String idOrName) {
        try {
            return deviceService.getDeviceByNameOrId(idOrName);
        } catch (DeviceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/name")
    @ResponseStatus(HttpStatus.OK)
    public boolean nameIsValidAndFree(@RequestBody Map<String, String> body) {
        try {
            return deviceService.checkIfDeviceNameIsFree(body.get("name"));
        } catch (InvalidDeviceNameException | IllegalArgumentException ide) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ide.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Device register(@RequestBody Device device) {

        try {
            return deviceService.register(device);
        } catch (InvalidDeviceNameException | IllegalArgumentException ide) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ide.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @PostMapping("/clone")
    @ResponseStatus(HttpStatus.OK)
    public Device cloneDevice(@RequestBody Map<String, Object> body) {

        try {
            final ObjectMapper mapper = new ObjectMapper();
            Device device = mapper.convertValue(body.get("newDevice"), Device.class);
            return deviceService.clone((String) body.get("oldDeviceId"), (String) body.get("oldDeviceSecretKey"), device);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException dne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, dne.getMessage());
        } catch (BadDeviceSecretKeyException bse) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, bse.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @GetMapping("/peer")
    @ResponseStatus(HttpStatus.OK)
    public List<Device> getPeers() {

        try {
            return deviceService.getPeerDevices(authenticationService.getCurrentUserId());
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/peer")
    @ResponseStatus(HttpStatus.CREATED)
    public Peer peerRequest(@RequestBody Map<String, String> body) {

        try {

            return deviceService.peering(authenticationService.getCurrentUserId(), body.get("applicantDeviceId"));
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException dne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, dne.getMessage());
        } catch (PeerConflictException pce) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, pce.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @PostMapping("/peer/accept")
    @ResponseStatus(HttpStatus.OK)
    public void acceptPeerRequest(@RequestBody Map<String, Object> body) {

        try {
            deviceService.acceptPeering((String) body.get("requesterId"), authenticationService.getCurrentUserId(), (Boolean) body.get("accept"));
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException dne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, dne.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @DeleteMapping("/peer/{peerDeviceId}")
    @ResponseStatus(HttpStatus.OK)
    public void dissociate(@PathVariable("peerDeviceId") String peerDeviceId) {

        try {
            deviceService.dissociate(authenticationService.getCurrentUserId(), peerDeviceId);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(
            value = "/photo/{deviceId}",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public byte[] getDevicePhoto(@PathVariable("deviceId") String deviceId) {
        try {
            return deviceService.getPhoto(deviceId);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException dne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, dne.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/photo")
    @ResponseStatus(HttpStatus.OK)
    public void updateDevicePhoto(@RequestParam("file") MultipartFile multipartFile) {

        try {
            deviceService.updatePhoto(authenticationService.getCurrentUserId(), multipartFile);
        } catch (IllegalArgumentException | InvalidFileType iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DeviceNotFoundException dne) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, dne.getMessage());
        } catch (FileTooLargeException fle) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, fle.getMessage());
        } catch (Exception e) {
            // TODO sent email to admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

}
