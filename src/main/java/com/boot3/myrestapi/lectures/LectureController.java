package com.boot3.myrestapi.lectures;

import com.boot3.myrestapi.common.dto.ErrorsResource;
import com.boot3.myrestapi.lectures.dto.LectureReqDto;
import com.boot3.myrestapi.lectures.dto.LectureResDto;
import com.boot3.myrestapi.lectures.dto.LectureResource;
import com.boot3.myrestapi.lectures.validator.LectureValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

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

//    @GetMapping
//    public ResponseEntity<?> queryLectures(Pageable pageable) {
//        Page<Lecture> lecturePage = this.lectureRepository.findAll(pageable);
//        return ResponseEntity.ok(lecturePage);
//    }

    @GetMapping
    public ResponseEntity<?> queryLectures(Pageable pageable,
                                        PagedResourcesAssembler<LectureResDto> assembler) {
        Page<Lecture> page = this.lectureRepository.findAll(pageable);
        Page<LectureResDto> lectureResDtoPage = page.map(lecture -> modelMapper.map(lecture, LectureResDto.class));
        //PagedModel<EntityModel<LectureResDto>> pagedResources = assembler.toModel(lectureResDtoPage);
        //assembler.toModel(lectureResDtoPage, resDto -> new LectureResource(resDto));
        PagedModel<LectureResource> pagedResources = assembler.toModel(lectureResDtoPage, LectureResource::new);
        return ResponseEntity.ok(pagedResources);
    }

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
        //Entity => ResDto 변환
        LectureResDto lectureResDto = modelMapper.map(addLecture, LectureResDto.class);

        WebMvcLinkBuilder selfLinkBuilder = linkTo(LectureController.class)
                .slash(addLecture.getId());
        URI createUri = selfLinkBuilder.toUri();

        LectureResource lectureResource = new LectureResource(lectureResDto);
        lectureResource.add(linkTo(LectureController.class).withRel("query-lectures"));
        lectureResource.add(selfLinkBuilder.withRel("update-lecture"));
        return ResponseEntity.created(createUri).body(lectureResource);
    }

    private static ResponseEntity<ErrorsResource> getErrors(Errors errors) {
        return ResponseEntity.badRequest().body(new ErrorsResource(errors));
    }
}