package com.led.broker.service;


import com.google.gson.Gson;
import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Topico;
import com.led.broker.repository.CorRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.util.ConfiguracaoUtil;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComandoService {

    private final MqttService mqttService;
    private final DispositivoRepository dispositivoRepository;
    private final AgendaDeviceService agendaDeviceService;
    private final CorRepository corRepository;
    private final LogRepository logRepository;
    public static Map<String, MonoSink<String>> streams = new HashMap<>();


    public Mono<String> createMono(String mac) {
        return Mono.create(sink -> {
            System.out.println(mac);
            streams.put(mac, sink);
        });
    }

    public Mono<String> enviardComandoTeste(String mac) {
        Dispositivo dispositivo = buscarPorMac(mac);

        Mono<String> mono = createMono(mac);

        if (dispositivo != null && dispositivo.getConfiguracao() != null) {
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComandoTeste(dispositivo.getConfiguracao()));
        }

        return mono;
    }

    public Mono<String> enviardComandoSincronizar(String mac, boolean responder) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

        if(!dispositivoOptional.isPresent()){
            return Mono.just(mac + " não encontrado ou inativo ");
        }

        Dispositivo dispositivo = dispositivoOptional.get();
        Mono<String> mono = createMono(mac);

        if (dispositivo.isAtivo() && dispositivo.getConfiguracao() != null) {
            dispositivo.setCor(getCor(dispositivo));
            if(dispositivo.getCor() != null){
                mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComando(dispositivo, responder));
                if(!responder){
                    return mono.just("ok");
                }
            }else{
                return mono.just("não possui configuração de cor");
            }

        }
        return mono;
    }


    public Mono<String> enviardComandoRapido(Dispositivo dispositivo, boolean cancelar, boolean interno) {

        Mono<String> mono = Mono.empty();

        if(!interno){
            mono = createMono(dispositivo.getMac());
        }

        if (cancelar) {
            dispositivo.setCor(getCor(buscarPorMac(dispositivo.getMac())));
        }

        if (dispositivo.isAtivo() && dispositivo.getConfiguracao() != null && dispositivo.getCor() != null) {
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComando(dispositivo, true));
        }


        return mono;
    }

    public String enviarComandoTodos() {

        try {
            List<Dispositivo> dispositivos = listaTodosDispositivos();

            if (!dispositivos.isEmpty()) {

                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("request.getUsuario()")
                        .mensagem("Todos")
                        .cor(null)
                        .comando(Comando.SINCRONIZAR)
                        .descricao(Comando.SINCRONIZAR.value())
                        .mac("Todos ativos")
                        .build());

                dispositivos.forEach(device -> {
                    if (device.isAtivo() && device.getConfiguracao() != null) {
                        device.setCor(getCor(device));
                        mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getMac(), ConfiguracaoUtil.gerarComando(device));
                        System.out.println(new Gson().toJson(device.getConfiguracao()));
                    }
                });
            } else {
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("request.getUsuario()")
                        .cor(null)
                        .mensagem("Nenhum dos dispositos estão ativos")
                        .comando(Comando.NENHUM_DEVICE)
                        .descricao(Comando.NENHUM_DEVICE.value())
                        .build());
            }
            return "Comando enviado para todos";
        }catch (Exception erro){
            return "Sincronização não foi concluida";
        }
    }

    public void enviarComando(List<String> macs, boolean forcaTeste, boolean sincronizar) {

        List<Dispositivo> dispositivos = todosDispositivosAtivos(macs, true);

        if (!dispositivos.isEmpty()) {

            if (sincronizar) {
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("Leandro")
                        .mensagem(forcaTeste ? "Especifico" : "Todos")
                        .cor(null)
                        .comando(Comando.SINCRONIZAR)
                        .descricao(Comando.SINCRONIZAR.value())
                        .mac(macs.toString())
                        .build());
            }


            dispositivos.forEach(device -> {

                boolean salvarLog = true;

                if (device.isAtivo() && device.getConfiguracao() != null) {
                    if (!forcaTeste) {
                        device.setCor(getCor(device));
                    }
                    if(device.getCor() != null){
                        mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getMac(), ConfiguracaoUtil.gerarComando(device));
                        System.out.println(new Gson().toJson(device.getConfiguracao()));
                    }
                }

                if (forcaTeste) {
                    logRepository.save(Log.builder()
                            .data(LocalDateTime.now())
                            .usuario("Leandro")
                            .mensagem("Tesde configuração enviado")
                            .cor(null)
                            .comando(Comando.TESTE)
                            .descricao(Comando.TESTE.value())
                            .mac(device.getMac())
                            .build());
                }
            });
            logRepository.save(Log.builder()
                    .data(LocalDateTime.now())
                    .usuario("Leandro")
                    .mensagem("Enviado para todos")
                    .cor(null)
                    .comando(Comando.ENVIAR)
                    .mac(macs.toString())
                    .descricao(Comando.ENVIAR.value())
                    .build());
        } else {
            logRepository.save(Log.builder()
                    .data(LocalDateTime.now())
                    .usuario("Leandro")
                    .cor(null)
                    .mensagem(macs.toString())
                    .comando(Comando.NENHUM_DEVICE)
                    .descricao(Comando.NENHUM_DEVICE.value())
                    .build());
        }
    }

    private Cor getCor(Dispositivo dispositivo) {
        Agenda agenda = null;

        if (TimeUtil.isTime(dispositivo)) {
            Optional<Cor> corOptional = buscaCor(dispositivo.getTemporizador().getIdCor());
            if (corOptional.isPresent()) {
                return corOptional.get();
            }
        }
        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda())) {
            agenda = agendaDeviceService.buscarAgendaDipositivoPrevistaHoje(dispositivo.getMac());
        }
        if (agenda != null && agenda.getCor() != null) {
            return agenda.getCor();
        }
        return dispositivo.getCor();
    }

    public void enviarComando(Agenda agenda) {

        if(agenda.getCor() != null) {
            List<Dispositivo> dispositivos = Collections.EMPTY_LIST;

            if (agenda.isTodos()) {
                dispositivos = dispositivoRepository.findAllByAtivoIgnorarAgenda(true, false);
            } else {
                dispositivos = agenda.getDispositivos()
                        .stream()
                        .filter(device -> device.isAtivo() && device.getConfiguracao() != null)
                        .collect(Collectors.toList());
            }

            if (!dispositivos.isEmpty()) {
                dispositivos.forEach(device -> {

                    if (device.isAtivo() && device.getConfiguracao() != null) {
                        if (Boolean.FALSE.equals(device.isIgnorarAgenda()) && !TimeUtil.isTime(device)) {
                            device.setCor(agenda.getCor());
                            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getMac(),
                                    new Gson().toJson(ConfiguracaoUtil.gerarComando(device)), false);
                        }
                    }
                });
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario(Comando.SISTEMA.value())
                        .mensagem("Tarefa agenda executada")
                        .cor(agenda.getCor())
                        .comando(Comando.SISTEMA)
                        .descricao("Tarefa agenda executada")
                        .mac(agenda.getDispositivos().stream().map(mac -> mac.getMac()).collect(Collectors.toList()).toString())
                        .build());
            }
        }
    }

    public Optional<Cor> buscaCor(UUID id) {
        return corRepository.findById(id);
    }

    private Dispositivo buscarPorMac(String mac) {

        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);
        if(dispositivoOptional.isPresent()){
            return dispositivoOptional.get();
        }
        else if(ComandoService.streams.containsKey(mac)){
            ComandoService.streams.remove(mac).success( mac + " não encontrado ou inativo ");
        }
        return null;
    }

    private List<Dispositivo> listaTodosDispositivos() {
        return dispositivoRepository.findAllByAtivo(true);
    }

    private List<Dispositivo> todosDispositivos(List<String> macs) {
        return dispositivoRepository.findAllById(macs);
    }

    private List<Dispositivo> todosDispositivosAtivos(List<String> macs, boolean ativo) {
        return dispositivoRepository.findAllByMacInAndAtivo(macs, ativo);
    }

    public List<Dispositivo> buscarDispositivosAtivosComAgendaPesquisada() {
        List<Dispositivo> dispositivos = dispositivoRepository.findAllByAtivo(true);
        if (!dispositivos.isEmpty()) {
            dispositivos.forEach(device -> {
                Agenda agenda = agendaDeviceService.buscarAgendaDipositivoPrevistaHoje(device.getMac());
                if (agenda != null && agenda.getCor() != null) {
                    device.setCor(agenda.getCor());
                }
            });
        }
        return dispositivos;
    }
}
