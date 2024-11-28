package com.led.broker.repository;

import com.led.broker.model.Dispositivo;
import com.led.broker.model.constantes.Comando;
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
    @Query("{ 'ativo': ?0, 'ignorarAgenda': ?1, 'comando': ?2, 'configuracao': { $ne: null } }")
    List<Dispositivo> findAllByAtivoIgnorarAgendaOnline(boolean ativo, boolean ignorarAgenda, Comando comando);
    @Query("{ 'ativo': ?0, 'configuracao': { $ne: null } }")
    Page<Dispositivo> findAllByAtivo(boolean ativo, Pageable pageable);
    @Query("{ 'ativo': ?0 }")
    Page<Dispositivo> findAllByInativo(boolean ativo, Pageable pageable);
    @Query("{ 'ativo' : true, 'ultimaAtualizacao' : { $lt: ?0 }, 'configuracao': { $ne: null } }")
    List<Dispositivo> findAllAtivosComUltimaAtualizacaoAntes(Date dataLimite);
    @Query("{ 'ativo' : true, 'ultimaAtualizacao' : { $lt: ?0 }, 'configuracao': { $ne: null } }")
    Page<Dispositivo> findAllAtivosComUltimaAtualizacaoAntes(Date dataLimite, Pageable pageable);
    @Query("{ 'ativo' : true, 'ultimaAtualizacao' : { $lt: ?0 }, 'status' : 'Online', 'configuracao': { $ne: null } }")
    List<Dispositivo> findAllAtivosComUltimaAtualizacaoAntesQueEstavaoOnline(Date dataLimite);

    @Query("{ 'configuracao': null }")
    List<Dispositivo> findDispositivosSemConfiguracao();
    @Query("{ 'configuracao': null }")
    Page<Dispositivo> findDispositivosSemConfiguracao(Pageable pageable);

    @Query("{" +
            "   $or: [" +
            "       { 'mac': ?0 }," +
            "       { 'nome': { $regex: ?0, $options: 'i' } }," +
            "       { 'enderecoCompleto': { $regex: ?0, $options: 'i' } }" +
            "   ]," +
//            "   'ativo': true" +
            "}")
    Page<Dispositivo> findByMacAndNomeContaining(String texto, Pageable pageable);

}
