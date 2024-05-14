package com.dndoz.PosePicker.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyPoseResponse {
	private Long uploadCount;
	private Long bookmarkCount;

	public MyPoseResponse(Long uploadCount, Long bookmarkCount) {
		this.uploadCount = uploadCount;
		this.bookmarkCount = bookmarkCount;
	}
}
