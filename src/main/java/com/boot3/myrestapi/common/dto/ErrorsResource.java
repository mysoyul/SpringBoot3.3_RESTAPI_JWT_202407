package com.boot3.myrestapi.common.dto;

import com.boot3.myrestapi.common.IndexController;
import org.springframework.hateoas.EntityModel;
import org.springframework.validation.Errors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class ErrorsResource extends EntityModel<Errors> {
    private final Errors errors;

    public ErrorsResource(Errors content) {
        this.errors = content;
        add(linkTo(methodOn(IndexController.class).index()).withRel("index"));
    }

}