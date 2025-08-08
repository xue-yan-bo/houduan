package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;

@Data
public class TitleVolumeAnalyse {
    private List<DayTitleVolume> dayTitleVolumeList;

    private List<AverageDurationAnalyse> averageDurationAnalyseList;
}
