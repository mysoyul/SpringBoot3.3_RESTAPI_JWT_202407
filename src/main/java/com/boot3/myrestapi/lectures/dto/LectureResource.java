package com.boot3.myrestapi.lectures.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Getter
public class LectureResource extends RepresentationModel<LectureResource> {
    private final LectureResDto lectureResDto;
    
    public LectureResource(LectureResDto resDto) {
        this.lectureResDto = resDto;
    }
    

}