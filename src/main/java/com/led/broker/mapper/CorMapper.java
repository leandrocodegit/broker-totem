package com.led.broker.mapper;

import com.led.broker.controller.request.CorRequest;
import com.led.broker.controller.response.CorResponse;
import com.led.broker.model.Cor;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface CorMapper {


    Cor toEntity(CorRequest request);
    CorResponse toResume(Cor entity);
    CorResponse toResponse(Cor entity);

}
