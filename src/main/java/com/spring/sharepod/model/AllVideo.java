package com.spring.sharepod.model;

import com.spring.sharepod.v1.dto.response.BoardResponseDto;
import com.spring.sharepod.v1.dto.response.VideoAll;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AllVideo {
    private String result;
    private String msg;
    private List<VideoAll> videoData;
}
