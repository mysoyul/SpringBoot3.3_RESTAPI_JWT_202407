package com.boot3.myrestapi.lectures;

import com.boot3.myrestapi.common.dto.ErrorsResource;
import com.boot3.myrestapi.common.exception.BusinessException;
import com.boot3.myrestapi.lectures.dto.LectureReqDto;
import com.boot3.myrestapi.lectures.dto.LectureResDto;
import com.boot3.myrestapi.lectures.dto.LectureResource;
import com.boot3.myrestapi.lectures.validator.LectureValidator;
import com.boot3.myrestapi.security.userinfos.CurrentUser;
import com.boot3.myrestapi.security.userinfos.UserInfo;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> getLecture(@PathVariable Integer id,
                                        @CurrentUser UserInfo currentUser) {
//        Optional<Lecture> optionalLecture = this.lectureRepository.findById(id);
//        if(optionalLecture.isEmpty()) {
//            return ResponseEntity.notFound().build();
//        }
//        Lecture lecture = optionalLecture.get();

        Lecture lecture = this.lectureRepository.findById(id) //Optional<Lecture>
            .orElseThrow(() -> new BusinessException("Lecture Not Found", HttpStatus.NOT_FOUND ));
        LectureResDto lectureResDto = modelMapper.map(lecture, LectureResDto.class);

        if (lecture.getUserInfo() != null)
            lectureResDto.setEmail(lecture.getUserInfo().getEmail());
        LectureResource lectureResource = new LectureResource(lectureResDto);
        //인증토큰의 email과 Lecture가 참조하는 email주소가 같으면 update 링크를 제공하기
        if ((lecture.getUserInfo() != null) && (lecture.getUserInfo().equals(currentUser))) {
        //if ((lecture.getUserInfo() != null) && (lecture.getUserInfo() == currentUser)) {
            lectureResource.add(linkTo(LectureController.class)
                    .slash(lecture.getId()).withRel("update-lecture"));
        }
        return ResponseEntity.ok(lectureResource);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> queryLectures(Pageable pageable,
                                           PagedResourcesAssembler<LectureResDto> assembler,
                                           @CurrentUser UserInfo currentUser) {
        Page<Lecture> page = this.lectureRepository.findAll(pageable);
        //Page<LectureResDto> lectureResDtoPage = page.map(lecture -> modelMapper.map(lecture, LectureResDto.class));
        Page<LectureResDto> lectureResDtoPage =
                page.map(lecture -> {
                    LectureResDto lectureResDto = new LectureResDto();
                    if (lecture.getUserInfo() != null) {
                        lectureResDto.setEmail(lecture.getUserInfo().getEmail());
                    }
                    modelMapper.map(lecture, lectureResDto);
                    return lectureResDto;
                });
        //PagedModel<EntityModel<LectureResDto>> pagedResources = assembler.toModel(lectureResDtoPage);
        //assembler.toModel(lectureResDtoPage, resDto -> new LectureResource(resDto));
        PagedModel<LectureResource> pagedResources = assembler.toModel(lectureResDtoPage, LectureResource::new);

        if (currentUser != null) {
            pagedResources.add(linkTo(LectureController.class).withRel("create-Lecture"));
        }
        return ResponseEntity.ok(pagedResources);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLecture(@PathVariable Integer id,
                                           @RequestBody @Valid LectureReqDto lectureReqDto,
                                           Errors errors,
                                           @CurrentUser UserInfo currentUser  ) {

        String errMsg = String.format("Id = %d Lecture Not Found", id);
        Lecture existingLecture = this.lectureRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(errMsg, HttpStatus.NOT_FOUND));

        //Lecture가 참조하는 UserInfo 객체와 인증한 UserInfo 객체가 다르면 403 인증 오류
        if((existingLecture.getUserInfo() != null) && (!existingLecture.getUserInfo().equals(currentUser))) {
            throw new AccessDeniedException("등록한 User와 수정을 요청한 User가 다릅니다.");
            //return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        //입력항목 체크
        if (errors.hasErrors()) {
            return getErrors(errors);
        }
        //biz logic 입력항목 체크
        lectureValidator.validate(lectureReqDto, errors);
        if (errors.hasErrors()) {
            return getErrors(errors);
        }
        //ReqDto -> Entity
        this.modelMapper.map(lectureReqDto, existingLecture);
        //free, offline 값 업데이트
        existingLecture.update();

        Lecture savedLecture = this.lectureRepository.save(existingLecture);
        LectureResDto lectureResDto = modelMapper.map(savedLecture, LectureResDto.class);
        //Lecture 객체와 연관된 UserInfo 객체가 있다면 LectureResDto에 email을 set
        if(savedLecture.getUserInfo() != null)
            lectureResDto.setEmail(savedLecture.getUserInfo().getEmail());

        LectureResource lectureResource = new LectureResource(lectureResDto);
        return ResponseEntity.ok(lectureResource);
    }

    @PostMapping
    public ResponseEntity<?> createLecture(@RequestBody @Valid LectureReqDto lectureReqDto,
                                           Errors errors,
                                           @CurrentUser UserInfo currentUser) {
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

        //Lecture와 UserInfo 연관관계 설정
        lecture.setUserInfo(currentUser);

        Lecture addLecture = this.lectureRepository.save(lecture);
        //Entity => ResDto 변환
        LectureResDto lectureResDto = modelMapper.map(addLecture, LectureResDto.class);

        //LectureResDto 에 UserInfo 객체의 email set
        lectureResDto.setEmail(addLecture.getUserInfo().getEmail());

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