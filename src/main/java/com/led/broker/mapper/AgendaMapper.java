package com.led.broker.mapper;

import com.led.broker.controller.request.AgendaRequest;
import com.led.broker.controller.response.AgendaResponse;
import com.led.broker.model.Agenda;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface AgendaMapper {


    Agenda toEntity(AgendaRequest request);
    AgendaResponse toResponse(Agenda entity);

}
