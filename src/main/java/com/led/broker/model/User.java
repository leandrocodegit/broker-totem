package com.led.broker.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@Builder
@Document(collection = "users")
public class User {

    @Id
    private UUID id;
    @DBRef
    private Cliente cliente;
    private String email;

}
