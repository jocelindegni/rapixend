package com.saankaa.rapidxend.repository;

import com.saankaa.rapidxend.model.DataBlock;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IDataBlockRepository extends MongoRepository<DataBlock, String> {
}
