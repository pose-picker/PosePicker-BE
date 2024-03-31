package com.dndoz.PosePicker.Service;

import com.dndoz.PosePicker.Domain.User;
import com.dndoz.PosePicker.Repository.UserRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.dndoz.PosePicker.Domain.PoseInfo;
import com.dndoz.PosePicker.Domain.PoseTag;
import com.dndoz.PosePicker.Domain.PoseTagAttribute;
import com.dndoz.PosePicker.Dto.PoseInfoResponse;
import com.dndoz.PosePicker.Dto.PoseTalkResponse;
import com.dndoz.PosePicker.Dto.PoseUploadRequest;
import com.dndoz.PosePicker.Dto.PoseUpdateRequest;
import com.dndoz.PosePicker.Repository.PoseFilterRepository;
import com.dndoz.PosePicker.Repository.PoseInfoRepository;
import com.dndoz.PosePicker.Repository.PoseTagAttributeRepository;
import com.dndoz.PosePicker.Repository.PoseTagRepository;
import com.dndoz.PosePicker.Repository.PoseTalkRepository;

@Service
public class AdminService {
	private final AmazonS3 amazonS3;
	private final UserRepository userRepository;
	private final PoseInfoRepository poseInfoRepository;
	private final PoseTalkRepository poseTalkRepository;
	private final PoseTagAttributeRepository poseTagAttributeRepository;
	private final PoseTagRepository poseTagRepository;
	private final PoseFilterRepository poseFilterRepository;

	public AdminService(AmazonS3 amazonS3, UserRepository userRepository, final PoseInfoRepository poseInfoRepository, final PoseTalkRepository poseTalkRepository, final PoseTagRepository poseTagRepository,
                        final PoseFilterRepository poseFilterRepository, final PoseTagAttributeRepository poseTagAttributeRepository) {
		this.amazonS3 = amazonS3;
        this.userRepository = userRepository;
        this.poseInfoRepository = poseInfoRepository;
		this.poseTalkRepository = poseTalkRepository;
		this.poseTagRepository = poseTagRepository;
		this.poseFilterRepository = poseFilterRepository;
		this.poseTagAttributeRepository = poseTagAttributeRepository;
	}
	   @Value("${cloud.aws.s3.bucketName}")
	   private String bucketName;

		@Transactional
		public PoseInfoResponse uploadPose(PoseUploadRequest poseDto, MultipartFile multipartFile) throws IOException {
			Long userId = 0L;
			User user = userRepository.findById(userId).orElseThrow(NullPointerException::new);

			if (!multipartFile.isEmpty()){
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
				poseInfo.setUser(user);

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

				return new PoseInfoResponse(savedPoseInfo);
			} else {
				return null;
			}
		}

	//포즈픽(사진) 조회
	public PoseInfoResponse showRandomPoseInfo(Long people_count) {
		PoseInfo poseInfo = poseFilterRepository.findRandomPoseInfo(people_count)
			.orElseThrow(NullPointerException::new);
		return new PoseInfoResponse(bucketName, poseInfo);
	}

	@Transactional
	public String updatePose(PoseUpdateRequest poseDto) {
		// try {
			Optional<PoseInfo> poseInfoOptional = poseInfoRepository.findByPoseId(poseDto.getPoseId());

			if (poseInfoOptional.isPresent()) {
				PoseInfo poseInfo = poseInfoOptional.get();
				// poseInfo.setImageKey(poseDto.getImageKey());
				poseInfo.setPeopleCount(Long.parseLong(poseDto.getPeopleCount()));
				poseInfo.setFrameCount(Long.parseLong(poseDto.getFrameCount()));
				poseInfo.setSource(poseDto.getSource());
				poseInfo.setSourceUrl(poseDto.getSourceUrl());

				poseTagRepository.deleteByPoseId(poseDto.getPoseId());
				// userRepository.deleteById(id);
				String[] tagsArray = poseDto.getTags().split(",");

				for (String tag : tagsArray) {
					Optional<PoseTagAttribute> tagAttributeOptional = poseTagAttributeRepository.findByPoseTagAttribute(tag);
					PoseTagAttribute tagAttribute = tagAttributeOptional.orElseGet(() -> {
						PoseTagAttribute newTagAttribute = new PoseTagAttribute();
						newTagAttribute.setAttribute(tag);
						return poseTagAttributeRepository.save(newTagAttribute);
					});

					PoseTag poseTag = new PoseTag(tagAttribute, poseInfo);
					poseTagRepository.save(poseTag);
				}
				poseInfoRepository.save(poseInfo);
				return "Pose 업데이트가 성공적으로 완료되었습니다.";
			} else {
				return "Pose 업데이트에 실패했습니다. 지정된 pose_id를 찾을 수 없습니다.";
			}
	}

	public List<PoseTalkResponse> getPoseTalk() {
		return poseTalkRepository.findAllPoseTalk()
			.stream()
			.map(poseTalk -> new PoseTalkResponse(poseTalk))
			.collect(Collectors.toList());

	}

	public void deletePose(Long poseId) {
		poseInfoRepository.deleteById(poseId);
	}
}
