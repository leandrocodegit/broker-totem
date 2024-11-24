package com.led.broker.service;

import com.led.broker.model.Agenda;
import com.led.broker.repository.AgendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgendaDeviceService {

    private final AgendaRepository agendaRepository;

    public List<Agenda> listaTodosAgendasPrevistaHoje() {
        LocalDate data = LocalDateTime.now().minusHours(3).toLocalDate();
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
