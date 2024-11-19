package com.led.broker.service;

import com.led.broker.mapper.AgendaMapper;
import com.led.broker.model.Agenda;
import com.led.broker.repository.AgendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AgendaDeviceService {

    @Autowired
    private AgendaRepository agendaRepository;
    @Autowired
    private AgendaMapper agendaMapper;

    public List<Agenda> listaTodosAgendasPrevistaHoje() {
        LocalDate data = LocalDateTime.now().plusHours(3).toLocalDate();
        return agendaRepository.findAgendasByDataDentroDoIntervalo(data);
    }
    public Agenda buscarAgendaDipositivoPrevistaHoje(String mac) {
        List<Agenda> agendaList = agendaRepository.findFirstByDataAndDispositivo(LocalDate.now(), LocalDate.now(), mac, UUID.randomUUID());
        if(!agendaList.isEmpty()){
            return agendaList.get(0);
        }
        return null;
    }

    public boolean possuiAgendaDipositivoPrevistaHoje(Agenda agenda, String mac) {
        return !agendaRepository.findFirstByDataAndDispositivo(agenda.getInicio(), agenda.getTermino(), mac, agenda.getId()).isEmpty();
    }

    public void atualizarDataExecucao(Agenda agenda) {
        agenda.setExecucao(LocalDate.now());
        agendaRepository.save(agenda);
    }
}
