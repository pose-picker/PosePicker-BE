package com.dndoz.PosePicker.Repository;

import java.util.List;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dndoz.PosePicker.Domain.PoseTagAttribute;

public interface PoseTagAttributeRepository extends JpaRepository<PoseTagAttribute, Long> {

	@Query(value = "SELECT * FROM tag_attribute LIMIT 10 OFFSET 1", nativeQuery = true)
	List<PoseTagAttribute> findPoseTagAttribute();

	@Query(value = "SELECT * FROM tag_attribute WHERE attribute = :attribute", nativeQuery = true)
	Optional<PoseTagAttribute> findByPoseTagAttribute(@Param("attribute") String attribute);
}
