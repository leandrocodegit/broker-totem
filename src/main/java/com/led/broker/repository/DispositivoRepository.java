package com.led.broker.repository;

import com.led.broker.model.Dispositivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoRepository extends MongoRepository<Dispositivo, Long> {


    Optional<Dispositivo> findAllByIdAndTopico(long id, long topico);

    List<Dispositivo> findAllByIdInAndAtivo(List<Long> ids, boolean ativo);

    @Query("{'cliente': { $ne: null }, 'cliente.id': ?0, 'ativo': ?1 }")
    List<Dispositivo> findAllByAtivo(UUID clienteId, boolean ativo);

    List<Dispositivo> findAllByCorVibracao(String id);

}
