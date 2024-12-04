package com.led.broker.repository;

import com.led.broker.model.Cor;
import com.led.broker.model.Operacao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface OperacaoRepository extends MongoRepository<Operacao, String> {}
