package com.jlm.homework.service;

import com.jlm.homework.entity.WrongTitleWriteData;

public interface IWrongTitleWriteDataService {
    void save(WrongTitleWriteData wrongTitleWriteData);

    WrongTitleWriteData getById(Long wrongTitleId);
}
