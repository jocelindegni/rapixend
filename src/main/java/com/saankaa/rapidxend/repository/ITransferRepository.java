package com.saankaa.rapidxend.repository;

import com.saankaa.rapidxend.model.Device;
import com.saankaa.rapidxend.model.Transfer;
import com.saankaa.rapidxend.model.TransferState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ITransferRepository extends MongoRepository<Transfer, String> {

    List<Transfer> findAllBySenderIsOrReceiversContains(Device device1, Device device2, Pageable pageable);

    Transfer findByIdAndReceiversContains(String id, Device device);

    Transfer findByIdAndSender_Id(String id, String sendId);

    List<Transfer> findByStateAndSender_Id(TransferState state, String deviceId);

}
