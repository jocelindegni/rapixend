package com.saankaa.rapidxend.repository;

import com.saankaa.rapidxend.model.File;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IFileRepository extends MongoRepository<File, String> {

}
