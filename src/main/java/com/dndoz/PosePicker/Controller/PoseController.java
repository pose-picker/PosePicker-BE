package com.dndoz.PosePicker.Controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dndoz.PosePicker.Dto.MyPoseResponse;
import com.dndoz.PosePicker.Dto.PoseFeedRequest;
import com.dndoz.PosePicker.Dto.PoseFeedResponse;
import com.dndoz.PosePicker.Dto.PoseInfoResponse;
import com.dndoz.PosePicker.Dto.PoseTagAttributeResponse;
import com.dndoz.PosePicker.Dto.PoseTalkResponse;
import com.dndoz.PosePicker.Dto.PoseUploadRequest;
import com.dndoz.PosePicker.Global.status.StatusResponse;
import com.dndoz.PosePicker.Service.PoseService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/api/pose")
@Api(tags = {"포즈 API"})
public class PoseController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final PoseService poseService;

	public PoseController(final PoseService poseService) {
		this.poseService = poseService;
	}

	/**
	 * @Description 포즈 이미지 상세 조회
	 * @param pose_id
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{pose_id}")
	@ApiResponses({@ApiResponse(code = 200, message = "포즈 사진 상세 조회 성공"),
	@ApiResponse(code = 401, message = "접근 권한이 없습니다.")})
	@ApiOperation(value = "포즈 사진 상세 조회", notes = "사진 클릭 시 포즈 상세 정보")
	public ResponseEntity<PoseInfoResponse> getPoseInfo(
		@RequestHeader(value = "Authorization", required = false) String accessToken, @PathVariable Long pose_id) throws
		IllegalAccessException {
		logger.info("[getPoseInfo] 포즈 사진 상세 조회 요청");
		return ResponseEntity.ok(poseService.getPoseInfo(accessToken,pose_id));
	}

	/**
	 * @Description 포즈 업로드
	 * @param peopleCount
	 * @param frameCount
	 * @param tags
	 * @param source
	 * @param sourceUrl
	 * @param description
	 * @param multipartFile
	 * @return
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@ApiResponse(code = 200, response = PoseInfoResponse.class, message = "포즈 데이터 업로드 성공")
	@ApiOperation(value = "포즈 데이터 업로드", notes = "포즈 사진 업로드")
	@PostMapping()
	public ResponseEntity<PoseInfoResponse> uploadPose(
		@RequestHeader(value= "Authorization", required=false) String accessToken,
		@RequestPart(value = "peopleCount") String peopleCount,
		@RequestPart(value = "frameCount") String frameCount,
		@RequestPart(value = "tags") String tags,
		@RequestPart(value = "source", required = false) String source,
		@RequestPart(value = "sourceUrl", required = false) String sourceUrl,
		@RequestPart(value = "description", required = false) String description,
		@RequestPart(value = "file") MultipartFile multipartFile) throws IOException, IllegalAccessException {
		PoseUploadRequest poseUploadRequest = new PoseUploadRequest(peopleCount, frameCount, tags, source, sourceUrl,
			description);
		return ResponseEntity.ok(poseService.uploadPose(accessToken, poseUploadRequest, multipartFile));
	}

	/**
	 * @Description 포즈픽 사진 조회
	 * @param people_count
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/pick/{people_count}")
	@ApiResponse(code = 200, message = "포즈픽 데이터 전달 성공")
	@ApiOperation(value = "포즈픽", notes = "포즈사진 랜덤픽")
	public ResponseEntity<PoseInfoResponse> showRandomPoseInfo(@PathVariable Long people_count) {
		logger.info("[showRandomPoseInfo] 포즈픽 데이터 요청");
		return ResponseEntity.ok(poseService.showRandomPoseInfo(people_count));
	}

	/**
	 * @Description 포즈톡 단어 조회
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/talk")
	@ApiResponse(code = 200, message = "포즈톡 데이터 전달 성공")
	@ApiOperation(value = "포즈톡", notes = "포즈단어 랜덤톡")
	public ResponseEntity<PoseTalkResponse> findRandomPoseTalk() {
		logger.info("[findRandomPoseTalk] 포즈톡 요청");
		return ResponseEntity.ok(poseService.findRandomPoseTalk());
	}

	/**
	 * @Description 포즈 태그 리스트 조회
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/tags")
	@ApiResponse(code = 200, message = "포즈 태그 리스트 전달 성공")
	@ApiOperation(value = "포즈 태그", notes = "필터링 드롭다운에 표시되는 태그 정보 \n - 9개 제공(v.0.0.1)")
	public ResponseEntity<PoseTagAttributeResponse> findPoseTagAttribute() {
		logger.info("[findPoseTagAttribute] 포즈 태그 속성 정보 요청");
		return ResponseEntity.ok(poseService.findPoseTagAttribute());
	}

	/**
	 * @Description 포즈 피드 전체 조회
	 * @return
	 */
	@GetMapping("/all")
	@ApiResponse(code = 200, message = "전체 포즈 피드 리스트 전달 성공")
	@ApiOperation(value = "포즈 피드", notes = "전체 포즈 피드 정보를 제공합니다.")
	public ResponseEntity<?> getPoses(
		@RequestHeader(value = "Authorization", required = false) String accessToken,
		@RequestParam final Integer pageNumber,
		@RequestParam final Integer pageSize,
		@RequestParam(value="sort", required = false) final String sort
		) throws IllegalAccessException {
		logger.info("[getPoses] 전체 포즈 정보 요청");
		Slice<PoseInfoResponse> poses = poseService.findPoses(accessToken, pageNumber, pageSize, sort);
		return ResponseEntity.ok(poses);
	}

	/**
	 * @Description 포즈 피드 필터링 조회
	 * @return
	 */
	@GetMapping()
	@ApiOperation(value = "포즈 피드 필터링 데이터", notes = "필터링 정보를 함께 넘겨주세요.")
	@ApiResponses({@ApiResponse(code = 200, message = "포즈 피드 리스트 전달 성공")})
	@ApiImplicitParams({@ApiImplicitParam(name = "pageNumber", value = "페이지 번호(default : 0)", dataType = "Integer"),
		@ApiImplicitParam(name = "peopleCount", value = "인원 수", dataType = "Long"),
		@ApiImplicitParam(name = "frameCount", value = "프레임 수", dataType = "Long"),
		@ApiImplicitParam(name = "tags", value = "태그", dataType = "List")

	})
	public ResponseEntity<PoseFeedResponse> getPoseFeed(
		@RequestHeader(value= "Authorization", required=false) String accessToken,
		@RequestParam(value = "pageNumber", required = false) final Integer pageNumber,
		@RequestParam(value = "peopleCount", required = false) final Long peopleCount,
		@RequestParam(value = "frameCount", required = false) final Long frameCount,
		@RequestParam(value = "tags", required = false) final String tags) throws IllegalAccessException {
		logger.info("[getPoseFeed] 포즈 피드 리스트 요청");

		return ResponseEntity.ok(
			poseService.getPoseFeed(accessToken, new PoseFeedRequest(pageNumber, peopleCount, frameCount, tags)));
	}

	@GetMapping("/user")
	@ApiOperation(value = "사용자 업로드한 포즈 이미지 조회", notes = "사용자가 업로드한 모든 포즈 이미지를 페이징하여 반환합니다.")
	public ResponseEntity<?> getUserPoses(
		@RequestHeader(value = "Authorization", required = false) String accessToken,
		@RequestParam final Integer pageNumber, @RequestParam final Integer pageSize) throws IllegalAccessException {
		logger.info("[getUserPoses] 사용자가 업로드한 포즈 이미지 조회 요청");
		Slice<PoseInfoResponse> poses = poseService.findUserUploadedPoses(accessToken, pageNumber, pageSize);
		return ResponseEntity.ok(poses);
	}

	/**
	 * @Description 마이포즈 (업로드, 저장 개수) 조회
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/user/mypose")
	@ApiResponse(code = 200, message = "마이포즈 조회 ")
	@ApiOperation(value = "마이포즈 (업로드, 저장 개수) 조회", notes = "마이포즈 조회 성공")
	public ResponseEntity<MyPoseResponse> myPoseCount(
		@RequestHeader(value= "Authorization", required=false) String accessToken) throws IllegalAccessException {
		logger.info("[myPoseCount] 마이포즈 업로드&저장 개수 조회 요청");
		return ResponseEntity.ok(poseService.myPoseCount(accessToken));
	}

	/**
	 * @Description 포즈 신고 하기
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@PostMapping("/report")
	@ApiResponse(code = 200, message = "포즈 신고하기 등록")
	@ApiOperation(value = "포즈 신고하기", notes = "포즈 신고 성공")
	public ResponseEntity<StatusResponse> reportPose(
		@RequestHeader(value= "Authorization", required=false) String accessToken,
		@RequestParam Long poseId, @RequestParam String content)  throws IllegalAccessException {
		logger.info("[poseReport] 포즈 신고하기 요청");
		return ResponseEntity.ok(poseService.reportPose(accessToken, poseId, content));
	}
}

