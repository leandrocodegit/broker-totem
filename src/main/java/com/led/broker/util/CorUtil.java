package com.led.broker.util;

import com.led.broker.integracao.model.Dispositivo;
import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.DispositivoEntity;
import com.led.broker.model.Parametro;
import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.repository.OperacaoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public class CorUtil {

    private final OperacaoRepository operacaoRepository;

    public CorUtil(OperacaoRepository operacaoRepository) {
        this.operacaoRepository = operacaoRepository;
    }


    public static Cor parametricarCorDispositivoOperacao(Cor cor, Dispositivo dispositivoEntity) {

        var corDispositivo = dispositivoEntity.getCor();

        corDispositivo.setParametros(corDispositivo.getParametros().stream().limit(4).sorted(Comparator.comparing(Parametro::getPino)).toList());
        cor.setParametros(cor.getParametros().stream().limit(corDispositivo.getParametros().size()).sorted(Comparator.comparing(Parametro::getPino)).toList());

        for (int i = 0; i < cor.getParametros().size(); i++) {
            var parametroCor = cor.getParametros().get(i);
            var parametroDispositivo = corDispositivo.getParametros().get(i);

            parametroCor.getConfiguracao().setTipoCor(parametroDispositivo.getConfiguracao().getTipoCor());
            parametroCor.setPino(parametroDispositivo.getPino());
            if (parametroDispositivo.getConfiguracao().getFaixa() < parametroCor.getConfiguracao().getFaixa())
                parametroCor.getConfiguracao().setFaixa(parametroDispositivo.getConfiguracao().getFaixa());
            parametroCor.getConfiguracao().setLeds(parametroDispositivo.getConfiguracao().getLeds());
            if (cor.getParametros().size() - 1 <= i)
                break;
        }

        var portasNaoVinculadas = dispositivoEntity.getCor().getParametros().stream().filter(porta -> !cor.getParametros().stream().map(Parametro::getPino).toList().contains(porta.getPino())).toList();

        var parametros = new ArrayList<Parametro>();
        cor.getParametros().forEach(parametro -> {
            parametros.add(parametro);
        });
        for (int i = 0; i < portasNaoVinculadas.size(); i++) {
            var porta = portasNaoVinculadas.get(i);
            porta.setCor(Parametro.apagado());
            porta.setCorrecao(Parametro.apagado());
            porta.setEfeito(Efeito.COLORIDO);
            parametros.add(porta);
        }
        cor.setParametros(parametros);
        return cor;
    }

    public static Cor parametricarCorDispositivo(Cor cor, DispositivoEntity dispositivoEntity) {
        var corDispositivo = dispositivoEntity.getCor();
        cor.setNome(corDispositivo.getNome());
        if (!dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR))
            cor.setVelocidade(corDispositivo.getVelocidade());

        if (cor == null || cor.getParametros().isEmpty())
            return dispositivoEntity.getCor();

        var portasNaoVinculadas = dispositivoEntity.getCor().getParametros().stream().filter(porta -> !cor.getParametros().stream().map(Parametro::getPino).toList().contains(porta.getPino()));

        portasNaoVinculadas.forEach(porta -> {
            porta.setCor(Parametro.apagado());
            porta.setCorrecao(Parametro.apagado());
            porta.setEfeito(Efeito.COLORIDO);
        });

        for (int i = 0; i < corDispositivo.getParametros().size(); i++) {
            var parametroCor = cor.getParametros().get(i);
            var parametroDispositivo = corDispositivo.getParametros().get(i);

            parametroDispositivo.getConfiguracao().setIntensidade(parametroCor.getConfiguracao().getIntensidade());

            parametroDispositivo.setCor(parametroCor.getCor());
            parametroDispositivo.setCorrecao(parametroCor.getCorrecao());
            parametroDispositivo.setEfeito(parametroCor.getEfeito());


            if (cor.getParametros().size() - 1 <= i)
                break;
        }
        parametrizarFaixa(corDispositivo);
        return corDispositivo;
    }

    public static void parametrizarFaixa(Cor cor) {

    }

    public Cor repararCor(Dispositivo dispositivoEntity) {
        if (Stream.of(ModoOperacao.OCORRENCIA, ModoOperacao.TEMPORIZADOR).anyMatch(modo -> dispositivoEntity.getOperacao().getModoOperacao().equals(modo))) {
            if (TimeUtil.isTime(dispositivoEntity)) {
                if (dispositivoEntity.getOperacao().getCorTemporizador() != null) {
                    return parametricarCorDispositivo(dispositivoEntity.getOperacao().getCorTemporizador(), dispositivoEntity);
                }
            }
        }

        if (Boolean.FALSE.equals(dispositivoEntity.isIgnorarAgenda()) && (dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.AGENDA) || dispositivoEntity.getOperacao().getAgenda() != null)) {
            Agenda agenda = dispositivoEntity.getOperacao().getAgenda();
            if (agenda != null && agenda.getCor() != null && agenda.isAtivo() && (agenda.getDispositivos().contains(dispositivoEntity.getId()) || agenda.isTodos())) {
                if (verificaSeAgendaValida(agenda, dispositivoEntity.getId())){
                    if(!dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.AGENDA)) {
                        dispositivoEntity.getOperacao().setModoOperacao(ModoOperacao.AGENDA);
                        operacaoRepository.save(dispositivoEntity.getOperacao());
                    }
                    return parametricarCorDispositivo(agenda.getCor(), dispositivoEntity);
                }

            }
        }

        dispositivoEntity.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);
        operacaoRepository.save(dispositivoEntity.getOperacao());
        parametrizarFaixa(dispositivoEntity.getCor());

        return dispositivoEntity.getCor();
    }

    public boolean verificaSeAgendaValida(Agenda agenda, long id) {

        if (!agenda.isAtivo() || agenda.getDispositivos() == null || agenda.getDispositivos().isEmpty())
            if (!agenda.isTodos())
                return false;
        var bool = agenda.getInicio().equals(LocalDate.now()) || agenda.getInicio().isBefore(LocalDate.now());
        if (bool)
            bool = agenda.getTermino().equals(LocalDate.now()) || agenda.getTermino().isAfter(LocalDate.now());
        if (bool) {
            return agenda.getDispositivos().contains(id) || agenda.isTodos();
        }
        return false;
    }

}
