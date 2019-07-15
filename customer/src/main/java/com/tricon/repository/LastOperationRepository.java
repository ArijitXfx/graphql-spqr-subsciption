package com.tricon.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.tricon.bean.LastOperation;

@Repository
public interface LastOperationRepository extends MongoRepository<LastOperation, String>{

}
