package com.led.broker.service;

import com.led.broker.model.Agenda;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.repository.AgendaRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.OperacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgendaDeviceService {

    private final AgendaRepository agendaRepository;
    private final DispositivoRepository dispositivoRepository;
    private final OperacaoRepository operacaoRepository;

    public List<Agenda> listaTodosAgendasPrevistaHoje() {
        LocalDate data = LocalDateTime.now().toLocalDate();
        return agendaRepository.findAgendasByDataDentroDoIntervalo(data);
    }
    public Agenda buscarAgendaDipositivoPrevistaHoje(String mac) {
        List<Agenda> agendaList = agendaRepository.findFirstByDataAndDispositivo(LocalDate.now(), LocalDate.now(), mac, UUID.randomUUID());
        if(!agendaList.isEmpty()){
            System.out.println("Com agenda");
            return agendaList.get(0);
        }
        System.out.println("Sem agenda");
        return null;
    }

    public boolean possuiAgendaDipositivoPrevistaHoje(Agenda agenda, String mac) {
        return !agendaRepository.findFirstByDataAndDispositivo(agenda.getInicio(), agenda.getTermino(), mac, agenda.getId()).isEmpty();
    }

    public void atualizarDataExecucao(Agenda agenda) {
        agenda.setExecucao(LocalDateTime.now().toLocalDate());
        agendaRepository.save(agenda);
    }

    public void atualizarOperacaoDispositivo(Agenda agenda, Dispositivo dispositivo){
        dispositivo.getOperacao().setModoOperacao(ModoOperacao.AGENDA);
        dispositivo.getOperacao().setAgenda(agenda);
        operacaoRepository.save(dispositivo.getOperacao());
    }
}
