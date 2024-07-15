package com.dndoz.PosePicker.Service;

import com.dndoz.PosePicker.Domain.PoseReport;
import com.dndoz.PosePicker.Domain.PoseTag;
import com.dndoz.PosePicker.Dto.MyPoseResponse;
import com.dndoz.PosePicker.Global.status.StatusCode;
import com.dndoz.PosePicker.Global.status.StatusResponse;
import com.dndoz.PosePicker.Repository.PoseReportRepository;
import com.dndoz.PosePicker.Repository.PoseTagRepository;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.dndoz.PosePicker.Auth.JwtTokenProvider;
import com.dndoz.PosePicker.Domain.PoseInfo;
import com.dndoz.PosePicker.Domain.PoseTagAttribute;
import com.dndoz.PosePicker.Domain.PoseTalk;
import com.dndoz.PosePicker.Domain.User;
import com.dndoz.PosePicker.Dto.PoseFeedRequest;
import com.dndoz.PosePicker.Dto.PoseFeedResponse;
import com.dndoz.PosePicker.Dto.PoseInfoResponse;
import com.dndoz.PosePicker.Dto.PoseTagAttributeResponse;
import com.dndoz.PosePicker.Dto.PoseTalkResponse;
import com.dndoz.PosePicker.Dto.PoseUploadRequest;
import com.dndoz.PosePicker.Repository.BookmarkRepository;
import com.dndoz.PosePicker.Repository.PoseFilterRepository;
import com.dndoz.PosePicker.Repository.PoseInfoRepository;
import com.dndoz.PosePicker.Repository.PoseTagAttributeRepository;
import com.dndoz.PosePicker.Repository.PoseTalkRepository;
import com.dndoz.PosePicker.Repository.UserRepository;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@Transactional(readOnly = true)
@Service
public class PoseService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final AmazonS3 amazonS3;
	private final UserRepository userRepository;
	private final PoseInfoRepository poseInfoRepository;
	private final PoseTalkRepository poseTalkRepository;
	private final PoseFilterRepository poseFilterRepository;
	private final PoseTagRepository poseTagRepository;
	private final PoseTagAttributeRepository poseTagAttributeRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final BookmarkRepository bookmarkRepository;
	private final PoseReportRepository poseReportRepository;

	@Value("${aws.image_url.prefix}")
	private String urlPrefix;

	private List<PoseInfo> filteredPoseInfo;
	private List<PoseInfo> recommendedPoseInfo;
	private List<PoseInfo> upLoadPoseInfo;

	@Value("${cloud.aws.s3.bucketName}")
	private String bucketName;

	public PoseService(AmazonS3 amazonS3,
					   final UserRepository userRepository,
					   final PoseInfoRepository poseInfoRepository,
					   final PoseTalkRepository poseTalkRepository,
					   final PoseFilterRepository poseFilterRepository,
					   final PoseTagRepository poseTagRepository,
					   final PoseTagAttributeRepository poseTagAttributeRepository,
					   final JwtTokenProvider jwtTokenProvider,
		final BookmarkRepository bookmarkRepository, final PoseReportRepository poseReportRepository ) {

		this.amazonS3 = amazonS3;
		this.userRepository = userRepository;
		this.poseInfoRepository = poseInfoRepository;
		this.poseTalkRepository = poseTalkRepository;
		this.poseFilterRepository = poseFilterRepository;
		this.poseTagRepository = poseTagRepository;
		this.poseTagAttributeRepository = poseTagAttributeRepository;
		this.jwtTokenProvider = jwtTokenProvider;
		this.bookmarkRepository = bookmarkRepository;
		this.poseReportRepository=poseReportRepository;
	}

	//포즈 이미지 상세 조회
	public PoseInfoResponse getPoseInfo(String accessToken, Long pose_id) throws IllegalAccessException {
		PoseInfo poseInfo = poseFilterRepository.findByPoseId(pose_id).orElseThrow(NullPointerException::new);

		if (null != accessToken) {
			String token = jwtTokenProvider.extractJwtToken(accessToken);
			if (!jwtTokenProvider.validateToken(token)) {
				return null;
			}
			Long userId = Long.valueOf(jwtTokenProvider.extractUid(token));
			boolean bookmarkCheck = bookmarkRepository.existsByUserIdAndPoseId(userId, pose_id).intValue() > 0;
			poseInfo.setBookmarkCheck(bookmarkCheck);
		}
		return new PoseInfoResponse(urlPrefix, poseInfo);
	}

	// 포즈 업로드
	@Transactional
	public PoseInfoResponse uploadPose(String accessToken, PoseUploadRequest poseDto, MultipartFile multipartFile) throws IOException, IllegalAccessException {
		String token = jwtTokenProvider.extractJwtToken(accessToken);
		if (!jwtTokenProvider.validateToken(token)) {
			return null;
		}
		Long userId = Long.valueOf(jwtTokenProvider.extractUid(token));
		//User user = userRepository.findById(userId).orElseThrow(NullPointerException::new);

		if (!multipartFile.isEmpty()) {
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(multipartFile.getSize());
			metadata.setContentType(multipartFile.getContentType());

			String imageKey = DigestUtils.sha256Hex(userId + LocalDate.now().toString() + System.currentTimeMillis()) + ".jpg";
			amazonS3.putObject(bucketName, imageKey, multipartFile.getInputStream(), metadata);

			PoseInfo poseInfo = new PoseInfo();
			String poseIdHash = String.valueOf(userId.hashCode() & Integer.MAX_VALUE); // Create a hash of the userId
			poseInfo.setPoseId(Long.parseLong(poseIdHash));
			poseInfo.setFrameCount(Long.parseLong(poseDto.getFrameCount()));
			poseInfo.setPeopleCount(Long.parseLong(poseDto.getPeopleCount()));
			poseInfo.setSourceUrl(poseDto.getSourceUrl());
			poseInfo.setSource(poseDto.getSource());
			poseInfo.setImageKey(imageKey);
			poseInfo.setUser(userId);
			poseInfo.setShow(false);

			PoseInfo savedPoseInfo = poseInfoRepository.save(poseInfo);

			String[] tags = poseDto.getTags().split(",");
			for (String tag : tags) {
				PoseTagAttribute poseTagAttribute = poseTagAttributeRepository.findByPoseTagAttribute(tag)
					.orElseGet(() -> {
						PoseTagAttribute newPoseTagAttribute = new PoseTagAttribute();
						newPoseTagAttribute.setAttribute(tag);
						return poseTagAttributeRepository.save(newPoseTagAttribute);
					});

				PoseTag poseTag = new PoseTag();
				poseTag.setPoseInfo(savedPoseInfo);
				poseTag.setPoseTagAttribute(poseTagAttribute);
				poseTagRepository.save(poseTag);
			}

			return new PoseInfoResponse(urlPrefix, new PoseInfo(savedPoseInfo, savedPoseInfo.getTagAttributes()));
		} else {
			return null;
		}
	}


	//포즈픽(사진) 조회
	public PoseInfoResponse showRandomPoseInfo(Long people_count) {
 		PoseInfo poseInfo = poseFilterRepository.findRandomPoseInfo(people_count)
			.orElseThrow(NullPointerException::new);
		return new PoseInfoResponse(urlPrefix, poseInfo);
	}

	//포즈톡(단어) 조회
	public PoseTalkResponse findRandomPoseTalk() {
		PoseTalk poseWord = poseTalkRepository.findRandomPoseTalk();
		return new PoseTalkResponse(poseWord);
	}

	//포즈 태그 속성 조회
	public PoseTagAttributeResponse findPoseTagAttribute() {
		List<PoseTagAttribute> poseTagAttributes = poseTagAttributeRepository.findPoseTagAttribute();
		return new PoseTagAttributeResponse(poseTagAttributes);
	}

	@Transactional
	public void setBookmarkStatusForPoses(Long userId) {
		//한 번의 조회로 여러 포즈에 대한 북마크 여부를 설정
		List<Object[]> bookmarkStatusList = poseInfoRepository.findBookmarkStatusByUserId(userId);
		Map<Long, Boolean> bookmarkStatusMap = new HashMap<>();

		for (Object[] result : bookmarkStatusList) {
			Long poseId = Long.valueOf(result[0].toString());
			boolean isBookmarked = ((BigInteger)result[1]).compareTo(BigInteger.ZERO) != 0;
			bookmarkStatusMap.put(poseId, isBookmarked);
		}
		List<PoseInfo> poses = poseInfoRepository.findPosesWithBookmarkStatus(userId);
		for (PoseInfo pose : poses) {
			boolean isBookmarked = bookmarkStatusMap.getOrDefault(pose.getPoseId(), false);
			pose.setBookmarkCheck(isBookmarked);
		}
		poseInfoRepository.saveAll(poses);
	}

	@Transactional(readOnly = true)
	public Slice<PoseInfoResponse> findPoses(String accessToken, final Integer pageNumber,
											 final Integer pageSize, final String sort) throws IllegalAccessException {
		// TODO: Enum 으로 정렬 조건 변경
		Sort sortOption = sort != null ? Sort.by(sort).descending() : Sort.by("created_at").ascending();
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sortOption);

		if (null != accessToken) {
			String token = jwtTokenProvider.extractJwtToken(accessToken);
			if (!jwtTokenProvider.validateToken(token)) {
				return null;
			}
			Long userId = Long.valueOf(jwtTokenProvider.extractUid(token));
			setBookmarkStatusForPoses(userId);
		}
		return poseInfoRepository.findPoses(pageable).map(poseInfo -> new PoseInfoResponse(urlPrefix, poseInfo));
	}

	@Transactional(readOnly = true)
	public PoseFeedResponse getPoseFeed(String accessToken, final PoseFeedRequest poseFeedRequest) throws
		IllegalAccessException {
		Long userId = 0L;
		if (null != accessToken) {
			logger.info("[getPoseFeed accessToken 존재-> 로그인 O]");
			String token = jwtTokenProvider.extractJwtToken(accessToken);
			if (!jwtTokenProvider.validateToken(token)) {
				return null;
			}
			userId = Long.valueOf(jwtTokenProvider.extractUid(token));
			//setBookmarkStatusForPoses(userId);
		}
		logger.info("[getPoseFeed accessToken 존재 X-> 로그인 X]");

		Pageable pageable = PageRequest.of(poseFeedRequest.getPageNumber(), poseFeedRequest.getPageSize());
		Slice<PoseInfoResponse> filteredContents;
		Slice<PoseInfo> slicedFilteredPoseInfo;
		List<PoseInfo> slicedFilteredResult;
		Slice<PoseInfoResponse> recommendedContents;
		Slice<PoseInfo> slicedRecommenededPoseInfo;
		List<PoseInfo> slicedRecommenededResult;

		if (poseFeedRequest.getTags() != null && poseFeedRequest.getTags().equals("")) {
			poseFeedRequest.setTags(null);
		}
		Boolean getRecommendationCheck = poseFilterRepository.getRecommendationCheck(poseFeedRequest.getPeopleCount(),
			poseFeedRequest.getFrameCount(), poseFeedRequest.getTags());

		if (poseFeedRequest.getPageNumber() == 0) {
			if (poseFeedRequest.getTags() == null) {
				logger.info("[태그 요청] tags is NULL");
				filteredPoseInfo = poseFilterRepository.findByFilterNoTag(pageable, poseFeedRequest.getPeopleCount(),
					poseFeedRequest.getFrameCount(), userId);
			} else {
				filteredPoseInfo = poseFilterRepository.findByFilter(pageable, poseFeedRequest.getPeopleCount(),
					poseFeedRequest.getFrameCount(), poseFeedRequest.getTags(), userId);
			}
		}

		Integer endIdx = Math.min(filteredPoseInfo.size(), (int)pageable.getOffset() + pageable.getPageSize());

		if ((int)pageable.getOffset() >= endIdx) {
			slicedFilteredResult = new ArrayList<>();
		} else {
			slicedFilteredResult = filteredPoseInfo.subList((int)pageable.getOffset(), endIdx);
		}

		slicedFilteredPoseInfo = new SliceImpl<>(slicedFilteredResult, pageable,
			slicedFilteredResult.size() == pageable.getPageSize());
		filteredContents = slicedFilteredPoseInfo.map(poseInfo -> new PoseInfoResponse(urlPrefix, poseInfo));

		if (getRecommendationCheck) {
			if (poseFeedRequest.getPageNumber() == 0) {
				recommendedPoseInfo = poseFilterRepository.getRecommendedContents(pageable, userId);
			}

			endIdx = Math.min(recommendedPoseInfo.size(), (int)pageable.getOffset() + pageable.getPageSize());

			if ((int)pageable.getOffset() >= endIdx) {
				slicedRecommenededResult = new ArrayList<>();
			} else {
				slicedRecommenededResult = recommendedPoseInfo.subList((int)pageable.getOffset(), endIdx);
			}

			slicedRecommenededPoseInfo = new SliceImpl<>(slicedRecommenededResult, pageable,
				slicedRecommenededResult.size() == pageable.getPageSize());
			recommendedContents = slicedRecommenededPoseInfo.map(poseInfo -> new PoseInfoResponse(urlPrefix, poseInfo));
			return new PoseFeedResponse(filteredContents, recommendedContents);
		}

		return new PoseFeedResponse(filteredContents);

	}

	@Transactional(readOnly = true)
	public Slice<PoseInfoResponse> findUserUploadedPoses(String accessToken, final Integer pageNumber, final Integer pageSize) throws IllegalAccessException {
		Pageable pageable = PageRequest.of(pageNumber, pageSize);

		if (null != accessToken) {
			String token = jwtTokenProvider.extractJwtToken(accessToken);
			if (!jwtTokenProvider.validateToken(token)) {
				return null;
			}
			Long uid = Long.valueOf(jwtTokenProvider.extractUid(token));

			upLoadPoseInfo = poseFilterRepository.findByUId(pageable, uid);
			boolean hasNext = false;
			if (upLoadPoseInfo.size() > pageSize) {
				upLoadPoseInfo.remove(upLoadPoseInfo.size() - 1);
				hasNext = true;
			}

			List<PoseInfoResponse> poseInfoResponses = upLoadPoseInfo.stream()
				.map(poseInfo -> new PoseInfoResponse(urlPrefix, poseInfo))
				.collect(Collectors.toList());

			return new SliceImpl<>(poseInfoResponses, pageable, hasNext);
		}

		return null;
	}

	public MyPoseResponse myPoseCount(String accessToken) throws IllegalAccessException {
		String token=jwtTokenProvider.extractJwtToken(accessToken);
		if (! jwtTokenProvider.validateToken(token)) {
			return null;
		}
		Long userId= Long.valueOf(jwtTokenProvider.extractUid(token));

		Long uploadCount=poseInfoRepository.findByUpLoadCountByUserId(userId);
		Long bookmarkCount=bookmarkRepository.findByBookmarkCountByUserId(userId);

		logger.info("[My Pose] 업로드 개수: " + uploadCount);
		logger.info("[My Pose] 북마크 개수: " + bookmarkCount);

		return new MyPoseResponse(uploadCount, bookmarkCount);
	}

	@Transactional
	public StatusResponse reportPose(String accessToken, Long poseId, String content) throws IllegalAccessException {
		String token = jwtTokenProvider.extractJwtToken(accessToken);
		if (!jwtTokenProvider.validateToken(token)) {
			return null;
		}
		Long uid = Long.valueOf(jwtTokenProvider.extractUid(token));
		// user id와 pose id 존재 여부 확인
		userRepository.findById(uid).orElseThrow(NullPointerException::new);
		poseInfoRepository.findByPoseId(poseId).orElseThrow(NullPointerException::new);

		// 1. 특정 pose에 한 사람당 한 번만 신고 가능
		if (poseReportRepository.existsByUserIdAndPoseId(uid, poseId).isPresent()) {
			return new StatusResponse(StatusCode.Forbidden,"이미 신고를 완료했습니다.");
		}

		// 2. 신고 기록 추가
		PoseReport poseReport = new PoseReport();
		poseReport.setUid(uid);
		poseReport.setPoseId(poseId);
		poseReport.setContent(content);
		poseReportRepository.save(poseReport);

		// 3. 특정 poseId에 대한 신고 개수 조회
		int reportCount = poseReportRepository.countByPoseId(poseId);

		logger.info("[Pose Report] 신고하기 poseId 개수: " + reportCount);

		// 4. 신고 개수가 3개 이상인 경우 poseInfo 테이블의 report 속성을 1로 변경
		if (reportCount >= 3) {
			poseInfoRepository.updateReportStatus(poseId);
		}
		return new StatusResponse(StatusCode.OK,"신고 완료");
	}

}
