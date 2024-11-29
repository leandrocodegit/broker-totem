package com.led.broker.service;

import com.led.broker.model.*;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.StatusConexao;
import com.led.broker.model.constantes.TipoCor;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DispositivoService {

    private final DispositivoRepository dispositivoRepository;

    public List<String> listaTodosDispositivos(boolean apenasOnline) {
        if (apenasOnline)
            return dispositivoRepository.findAllByAtivoEOnline(true).stream().map(device -> device.getMac()).toList();
        return dispositivoRepository.findAllByAtivo(true).stream().map(device -> device.getMac()).toList();
    }
}
