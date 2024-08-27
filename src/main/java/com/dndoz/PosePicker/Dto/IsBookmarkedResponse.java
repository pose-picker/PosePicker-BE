package com.dndoz.PosePicker.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IsBookmarkedResponse {
	private Long poseId;
	private boolean isBookmarked;
}
