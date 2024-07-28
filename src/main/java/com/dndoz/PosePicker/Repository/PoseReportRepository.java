package com.dndoz.PosePicker.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dndoz.PosePicker.Domain.PoseInfo;
import com.dndoz.PosePicker.Domain.PoseReport;

public interface PoseReportRepository extends JpaRepository<PoseReport, Long> {
	// 해당 포즈 신고 횟수
	@Query(value= "SELECT COUNT(*) FROM pose_report WHERE pose_id = :poseId" , nativeQuery = true)
    int countByPoseId(@Param("poseId") Long poseId);

	// 해당 포즈에 대한 사용자 신고 여부 확인
	@Query(value="SELECT * FROM pose_report WHERE uid = :userId and pose_id= :poseId" , nativeQuery = true)
	Optional<Boolean> existsByUserIdAndPoseId(@Param("userId") Long userId, @Param("poseId") Long poseId);

}
