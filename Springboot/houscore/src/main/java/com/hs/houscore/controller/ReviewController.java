package com.hs.houscore.controller;

import com.hs.houscore.dto.*;
import com.hs.houscore.response.ErrorResponse;
import com.hs.houscore.s3.S3UploadService;
import com.hs.houscore.postgre.entity.ReviewEntity;
import com.hs.houscore.postgre.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final S3UploadService s3UploadService;

    @Autowired
    public ReviewController(ReviewService reviewService, S3UploadService s3UploadService) {
        this.reviewService = reviewService;
        this.s3UploadService = s3UploadService;
    }

    @GetMapping("/list")
    @Operation(summary = "리뷰 전체 리스트", description = "리뷰 전체 리스트 반환")
    public ResponseEntity<?> getReviewList(@RequestParam(name = "page")Integer page, @RequestParam(name = "size")Integer size) {
        try {
            BuildingReviewDTO buildingReviewDTO = reviewService.getReviewList(page, size);
            if(buildingReviewDTO == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("ReviewController getReview NullException"));
            }
            return ResponseEntity.ok(buildingReviewDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("ReviewController getReview failure"));
        }
    }

    @GetMapping("")
    @Operation(summary = "리뷰 상세 내용", description = "리뷰 상세 내용 조회")
    public ResponseEntity<?> getReview(@RequestParam Long id) {
        try {
            ReviewEntity reviewEntity = reviewService.getDetailReview(id);
            if(reviewEntity == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("BuildingController getDetailReview NullException"));
            }
            return ResponseEntity.ok(reviewEntity);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("BuildingController getDetailReview failure"));
        }
    }

    @GetMapping("recent")
    @Operation(summary = "최근 리뷰 리스트", description = "최근 리뷰 리스트 조회")
    public ResponseEntity<?> getRecentReviews(){
        try {
            List<ReviewEntity> reviewEntities = reviewService.getRecentReviews();
            if(reviewEntities == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("BuildingController getRecentReviews NullException"));
            }else if (reviewEntities.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("BuildingController getRecentReviews is Empty"));
            }
            return ResponseEntity.ok(reviewEntities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("BuildingController getRecentReviews failure"));
        }
    }

    @GetMapping("my-review")
    @Operation(summary = "내가 쓴 리뷰 리스트", description = "내가 쓴 리뷰 리스트 조회")
    public ResponseEntity<?> getMyReviews(@AuthenticationPrincipal String memberEmail){
        try {
            //유저 검증
            if(memberEmail == null || memberEmail.equals("anonymousUser")){
                return ResponseEntity.badRequest().body(new ErrorResponse("사용자 검증 필요"));
            }

            List<ReviewEntity> reviewEntities = reviewService.getMyReviews(memberEmail);
            if(reviewEntities == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("BuildingController getMyReviews NullException"));
            }else if (reviewEntities.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("BuildingController getMyReviews is Empty"));
            }
            return ResponseEntity.ok(reviewEntities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("BuildingController getMyReviews failure"));
        }
    }

    @PostMapping("")
    @Operation(summary = "거주지 리뷰 등록", description = "거주지 리뷰 등록")
    public ResponseEntity<?> addReview(@RequestBody CreateReviewDTO review,
                                       @AuthenticationPrincipal String memberEmail) {
        System.out.println("일단 들어왔구연");
        try{
            System.out.println("good");
            //유저 검증
            if(memberEmail == null || memberEmail.equals("anonymousUser")) {
                return ResponseEntity.badRequest().body(new ErrorResponse("사용자 검증 필요"));
            }

            String imageName = memberEmail.split("@")[0]+ UUID.randomUUID() +".jpeg";
            System.out.println(imageName);

            // FileUploadDTO 세팅
            FileUploadDTO fileUploadDTO = new FileUploadDTO();
            fileUploadDTO.setImageBase64(review.getImages());
            fileUploadDTO.setImageName(imageName);
            System.out.println("DTO 세팅 이슈 아님");
            // S3 이미지 업로드
            String result = s3UploadService.saveImage(fileUploadDTO);
            review.setImages(result);
            System.out.println(result + "떴냐?");
            //이미지
            reviewService.setReview(review, memberEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body("리뷰 등록 성공");
        }catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 리뷰 데이터");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("리뷰 등록 실패!");
        }
    }

    @PutMapping("")
    @Operation(summary = "거주지 리뷰 수정", description = "거주지 리뷰 수정")
    public ResponseEntity<?> updateReview(@RequestBody ReviewDTO review,
                                          @AuthenticationPrincipal String memberEmail) {
        try{
            //유저 검증
            if(memberEmail == null || memberEmail.equals("anonymousUser")) {
                return ResponseEntity.badRequest().body(new ErrorResponse("사용자 검증 필요"));
            }
            if(review.getImageChange().equals("y")){
                String imageName = memberEmail.split("@")[0]+ UUID.randomUUID() +".jpeg";

                // FileUploadDTO 세팅
                FileUploadDTO fileUploadDTO = new FileUploadDTO();
                fileUploadDTO.setImageBase64(review.getImages());
                fileUploadDTO.setImageName(imageName);
                // S3 이미지 업로드
                String result = s3UploadService.saveImage(fileUploadDTO);
                review.setImages(result);
            }

            //이미지
            reviewService.updateReview(review, memberEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body("리뷰 수정 성공");
        }catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 리뷰 데이터");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("리뷰 수정 실패!");
        }
    }

    @DeleteMapping("")
    @Operation(summary = "거주지 리뷰 삭제", description = "거주지 리뷰 삭제")
    public ResponseEntity<?> deleteReview(@RequestParam Long id,
                                          @AuthenticationPrincipal String memberEmail) {
        try {
            //유저 검증
            if(memberEmail == null || memberEmail.equals("anonymousUser")){
                return ResponseEntity.badRequest().body(new ErrorResponse("사용자 검증 필요"));
            }

            reviewService.deleteReview(id, memberEmail);
            return ResponseEntity.status(HttpStatus.OK).body("리뷰 삭제 성공");
        }catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 리뷰 데이터");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("리뷰 삭제 실패!");
        }
    }
}
