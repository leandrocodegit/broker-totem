package com.led.broker.repository;

import com.led.broker.model.DispositivoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoRepository extends MongoRepository<DispositivoEntity, Long> {


    Optional<DispositivoEntity> findAllByIdAndTopico(long id, long topico);

    List<DispositivoEntity> findAllByIdInAndAtivo(List<Long> ids, boolean ativo);

    @Query("{'cliente': { $ne: null }, 'cliente.id': ?0, 'ativo': ?1 }")
    List<DispositivoEntity> findAllByAtivo(UUID clienteId, boolean ativo);

    List<DispositivoEntity> findAllByCorVibracao(String id);

}
