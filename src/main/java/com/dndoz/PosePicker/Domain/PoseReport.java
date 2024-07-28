package com.dndoz.PosePicker.Domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.dndoz.PosePicker.Global.entity.BaseEntity;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

@Entity(name="pose_report")
@Table(name = "pose_report")
@ApiModel(value = "포즈 신고: PoseReport")
@Getter
@Setter
public class PoseReport extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reportId;

	@Column(name = "uid")
	private Long uid;

	@Column(name = "pose_id")
	private Long poseId;

	@Column(name = "content")
	private String content;
}
