package com.boot3.myrestapi.lectures.validator;

import com.boot3.myrestapi.lectures.dto.LectureReqDto;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDateTime;

@Component
public class LectureValidator {
	public void validate(LectureReqDto lectureReqDto, Errors errors) {
		//maxPrice 0 보다 크면 basePrice > maxPrice 크면 오류 발생
		if(lectureReqDto.getBasePrice() > lectureReqDto.getMaxPrice() &&
				lectureReqDto.getMaxPrice() != 0) {
			//Field Error
			errors.rejectValue("basePrice", "wrongPrice", "BasePrice is wrong");
			errors.rejectValue("maxPrice", "wrongPrice", "MaxPrice is wrong");
			//Global Error
			errors.reject("wrongPrices", "BasePrice가 MaxPrice 보다 더 작은 값이어야 합니다.");
		}

		//강의종료날짜
		LocalDateTime endLectureDateTime = lectureReqDto.getEndLectureDateTime();

		if(endLectureDateTime.isBefore(lectureReqDto.getBeginLectureDateTime()) ||
		   endLectureDateTime.isBefore(lectureReqDto.getCloseEnrollmentDateTime()) ||
		   endLectureDateTime.isBefore(lectureReqDto.getBeginEnrollmentDateTime()) ) {
			errors.rejectValue("endLectureDateTime", "wrongDateTime",
					"endLectureDateTime(강의종료날짜)를 확인하세요!");
		}
		
		//강의시작날짜
		LocalDateTime beginLectureDateTime = lectureReqDto.getBeginLectureDateTime();

		if(beginLectureDateTime.isBefore(lectureReqDto.getCloseEnrollmentDateTime()) ||
		   beginLectureDateTime.isBefore(lectureReqDto.getBeginEnrollmentDateTime()) ||
		   beginLectureDateTime.isAfter(lectureReqDto.getEndLectureDateTime())) {
			errors.rejectValue("beginLectureDateTime", "wrongDateTime",
					"beginLectureDateTime(강의시작날짜)를 확인하세요!");
		}
	}
}