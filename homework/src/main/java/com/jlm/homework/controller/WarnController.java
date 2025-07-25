package com.jlm.homework.controller;


import com.jlm.homework.entity.Warn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "告警", description = "告警模拟")
@RestController
@RequestMapping("/api/warn")
public class WarnController {
    @GetMapping("/oneClickWarning")
    public Warn oneClickWarning() {
        Warn  warn = new Warn();
        warn.setProvince("山西省");
        warn.setCity("太原市");
        warn.setAdresse("太原小店区东中环南段259号亲海国际B座");
        warn.setLon("112.56878");
        warn.setLat("37.73565");
        return warn;
    }
}
