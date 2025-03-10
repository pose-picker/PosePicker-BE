package com.dndoz.PosePicker.Domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.dndoz.PosePicker.Global.entity.BaseEntity;

import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "pose_info")
@Table(name = "pose_info")
@ApiModel(value = "포즈 이미지 모델: PoseInfo")
@Getter
@Setter
public class PoseInfo extends BaseEntity {
	@Column(name = "image_key")
	String imageKey;
	@Column(name = "source")
	String source;
	@Column(name = "source_url")
	String sourceUrl;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long poseId;
	@Column(name = "people_count")
	private Long peopleCount;

	@Column(name = "frame_count")
	private Long frameCount;

	// @ManyToOne(fetch = FetchType.EAGER)
	// @JoinColumn(name = "uid")
	@Column(name = "uid")
	private Long user;

	@Column(name = "`show`" )
	private Boolean show;

	@Column(name = "`report`" )
	private Boolean report;

	@Transient
	private String tagAttributes;

	@Transient
	private boolean bookmarkCheck; //북마크 여부

	public PoseInfo(PoseInfo poseInfo, String tagAttributes) {
		this.poseId = poseInfo.getPoseId();
		this.imageKey = poseInfo.getImageKey();
		this.source = poseInfo.getSource();
		this.sourceUrl = poseInfo.getSourceUrl();
		this.poseId = poseInfo.getPoseId();
		this.peopleCount = poseInfo.getPeopleCount();
		this.frameCount = poseInfo.getFrameCount();
		this.user = poseInfo.getUser();
		this.show = poseInfo.getShow();
		this.report = poseInfo.getReport();
		this.tagAttributes = tagAttributes;
	}

	//포즈피드 필터링 시 북마크 여부 포함한 PoseInfo
	public PoseInfo(PoseInfo poseInfo, String tagAttributes, Boolean bookmarkCheck) {
		this.poseId = poseInfo.getPoseId();
		this.imageKey = poseInfo.getImageKey();
		this.source = poseInfo.getSource();
		this.sourceUrl = poseInfo.getSourceUrl();
		this.poseId = poseInfo.getPoseId();
		this.peopleCount = poseInfo.getPeopleCount();
		this.frameCount = poseInfo.getFrameCount();
		this.user = poseInfo.getUser();
		this.show = poseInfo.getShow();
		this.report = poseInfo.getReport();
		this.tagAttributes = tagAttributes;
		this.bookmarkCheck=bookmarkCheck;
	}

	public PoseInfo() {

	}

	public Boolean getShow() {
		return show;
	}

	public void setShow(Boolean show) {
		this.show = show;
	}

	public Long getPoseId() {
		return poseId;
	}

	public String getImageKey() {
		return imageKey;
	}

	public void setImageKey(String imageKey) {
		this.imageKey = imageKey;
	}

	public String getSource() {
		return source;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public Long getPeopleCount() {
		return peopleCount;
	}

	public Long getFrameCount() {
		return frameCount;
	}

	public String getTagAttributes() {
		return tagAttributes;
	}

	public boolean getBookmarkCheck() {
		return bookmarkCheck;
	}

	public void setBookmarkCheck(boolean bookmarkCheck) {
		this.bookmarkCheck = bookmarkCheck;
	}

}

