package com.boot3.myrestapi.lectures;

import com.boot3.myrestapi.lectures.dto.LectureReqDto;
import com.boot3.myrestapi.lectures.validator.LectureValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/lectures", produces = MediaTypes.HAL_JSON_VALUE)
@RequiredArgsConstructor
public class LectureController {
    private final LectureRepository lectureRepository;
    private final ModelMapper modelMapper;
    private final LectureValidator lectureValidator;
    
    //Constructor Injection 생성자주입
//    public LectureController(LectureRepository lectureRepository) {
//        this.lectureRepository = lectureRepository;
//    }

    @PostMapping
    public ResponseEntity<?> createLecture(@RequestBody @Valid LectureReqDto lectureReqDto,
                                           Errors errors) {
        //입력값 검증 오류가 있다면 400 오류 발생
        if(errors.hasErrors()) {
            return getErrors(errors);
        }

        //사용자정의 Validator  호출
        lectureValidator.validate(lectureReqDto, errors);
        //Biz 검증 있다면 400 오류 발생
        if(errors.hasErrors()) {
            return getErrors(errors);
        }
        
        //ReqDto => Entity 변환
        Lecture lecture = modelMapper.map(lectureReqDto, Lecture.class);
        //free와 offline 값을 update
        lecture.update();
        Lecture addLecture = this.lectureRepository.save(lecture);

        WebMvcLinkBuilder selfLinkBuilder = WebMvcLinkBuilder
                .linkTo(LectureController.class)
                .slash(addLecture.getId());
        URI createUri = selfLinkBuilder.toUri();
        return ResponseEntity.created(createUri).body(addLecture);
    }

    private static ResponseEntity<?> getErrors(Errors errors) {
        return ResponseEntity.badRequest().body(errors);
    }
}