package com.led.broker.util;

import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.Parametro;
import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.repository.OperacaoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CorUtil {

    private final OperacaoRepository operacaoRepository;

    public CorUtil(OperacaoRepository operacaoRepository) {
        this.operacaoRepository = operacaoRepository;
    }

    public static Cor parametricarCorDispositivo(Cor cor, Dispositivo dispositivo) {
        var corDispositivo = dispositivo.getCor();
        corDispositivo.setNome(cor.getNome());
        corDispositivo.setVelocidade(cor.getVelocidade());

        if(cor == null || cor.getParametros().isEmpty())
            return dispositivo.getCor();

        var portasNaoVinculadas = dispositivo.getCor().getParametros().stream().filter(porta -> !cor.getParametros().stream().map(Parametro::getPino).toList().contains(porta.getPino()));

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

    public Cor repararCor(Dispositivo dispositivo) {
        if (Stream.of(ModoOperacao.OCORRENCIA, ModoOperacao.TEMPORIZADOR).anyMatch(modo -> dispositivo.getOperacao().getModoOperacao().equals(modo))) {
            if (TimeUtil.isTime(dispositivo)) {
                if (dispositivo.getOperacao().getCorTemporizador() != null) {
                    return parametricarCorDispositivo(dispositivo.getOperacao().getCorTemporizador(), dispositivo);
                }
            }
        }

        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda()) && dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.AGENDA)) {
            Agenda agenda = dispositivo.getOperacao().getAgenda();
            if (agenda != null && agenda.getCor() != null && agenda.isAtivo() && agenda.getDispositivos().contains(dispositivo.getId())) {
                if (verificaSeAgendaValida(agenda, dispositivo.getId()))
                    return parametricarCorDispositivo(agenda.getCor(), dispositivo);
            }
        }

        dispositivo.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);
        operacaoRepository.save(dispositivo.getOperacao());
        parametrizarFaixa(dispositivo.getCor());

        return dispositivo.getCor();
    }

    public boolean verificaSeAgendaValida(Agenda agenda, long id) {

        if (!agenda.isAtivo() || agenda.getDispositivos() == null || agenda.getDispositivos().isEmpty())
            return false;
        var bool = agenda.getInicio().equals(LocalDate.now()) || agenda.getInicio().isBefore(LocalDate.now());
        if (bool)
            bool = agenda.getTermino().equals(LocalDate.now()) || agenda.getTermino().isAfter(LocalDate.now());
        ;
        if (bool) {
            return agenda.getDispositivos().contains(id);
        }
        return false;
    }

}
