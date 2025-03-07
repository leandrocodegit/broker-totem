package com.led.broker.service;

import com.led.broker.model.constantes.StatusConexao;
import com.led.broker.repository.DispositivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DispositivoService {

    private final DispositivoRepository dispositivoRepository;

    public List<Long> listaTodosDispositivos(boolean apenasOnline) {
        if (apenasOnline)
            return dispositivoRepository.findAllByAtivo(true).stream().filter(dispositivo -> dispositivo.getConexao().getStatus().equals(StatusConexao.Online)).map(device -> device.getId()).toList();
        return dispositivoRepository.findAllByAtivo(true).stream().map(device -> device.getId()).toList();
    }
}
