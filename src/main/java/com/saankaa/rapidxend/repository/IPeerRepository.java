package com.saankaa.rapidxend.repository;

import com.saankaa.rapidxend.model.Peer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface IPeerRepository extends MongoRepository<Peer, String> {

    Peer findByRequesterDevice_IdAndApplicantDevice_Id(String requesterDeviceId, String applicantDeviceId);

    List<Peer> findByRequesterDevice_IdOrApplicantDevice_Id(String requesterDeviceId, String applicantDeviceId);

}
