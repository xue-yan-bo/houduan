package com.jlm.homework.dto;

import com.jlm.homework.entity.StudentsHomeworkNew;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class StudentHomeworkDto {
    private Integer unsubmitted;
    private Integer submitted;
    private Integer total;
    private Page<StudentsHomeworkSimpleDTO> homeworkPage;
}
