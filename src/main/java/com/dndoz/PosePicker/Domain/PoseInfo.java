package com.dndoz.PosePicker.Domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "pose_info")
@Getter
@Setter

@ApiModel(value = "포즈 이미지 모델: PoseInfo")
public class PoseInfo {
	@Column(name = "image_key")
	String image_key;
	@Column(name = "source")
	String pose_source;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long pose_id;
	@Column(name = "people_count")
	private Integer people_count;

	@Column(name = "frame_count")
	private Integer frame_count;

}
