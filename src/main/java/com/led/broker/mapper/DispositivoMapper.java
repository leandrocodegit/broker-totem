package com.led.broker.mapper;

import com.led.broker.controller.response.DispositivoResponse;
import com.led.broker.model.Dispositivo;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface DispositivoMapper {


    DispositivoResponse toResponse(Dispositivo entity);

}
