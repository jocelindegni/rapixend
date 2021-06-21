package com.saankaa.rapidxend.repository;

import com.saankaa.rapidxend.model.Device;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface IDeviceRepository extends MongoRepository<Device, String> {

    Device findByName(String name);
}
