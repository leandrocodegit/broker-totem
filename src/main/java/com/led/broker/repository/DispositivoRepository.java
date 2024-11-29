package com.led.broker.repository;

import com.led.broker.model.Dispositivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;

public interface DispositivoRepository extends MongoRepository<Dispositivo, String> {



    List<Dispositivo> findAllByMacInAndAtivo(List<String> macs, boolean ativo);
    @Query("{ 'ativo': ?0, 'configuracao': { $ne: null } }")
    List<Dispositivo> findAllByAtivo(boolean ativo);
    @Query("{ 'ativo': ?0, 'configuracao': { $ne: null }, , 'conexao.status' : 'Online', }")
    List<Dispositivo> findAllByAtivoEOnline(boolean ativo);

}
