package com.led.broker.repository;

import com.led.broker.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends MongoRepository<User, UUID> {

    @Query("{" +
            "   $and: [" +
            "       { 'email': ?0 }," +
            "       { 'email': { $not: { $regex: 'master', $options: 'i' } } }" +
            "   ]" +
            "}")
    Optional<User> buscarPorEmail(String email);

}
