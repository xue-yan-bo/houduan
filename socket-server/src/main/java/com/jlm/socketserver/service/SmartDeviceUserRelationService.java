package com.jlm.socketserver.service;


import com.jlm.socketserver.entity.SmartDeviceUserRelation;
import com.jlm.socketserver.repository.SmartDeviceUserRelationRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SmartDeviceUserRelationService{
    @Resource
    private SmartDeviceUserRelationRepository smartDeviceUserRelationRepository;

    public SmartDeviceUserRelation update(SmartDeviceUserRelation deviceUserRelation) {
        return smartDeviceUserRelationRepository.save(deviceUserRelation);
    }

    public SmartDeviceUserRelation selectByDeviceCode(String mac) {
        SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
        relation.setDeviceCode(mac);
        SmartDeviceUserRelation deviceUserRelation=smartDeviceUserRelationRepository.findOne(Example.of(relation)).get();
        return deviceUserRelation;
    }


    public SmartDeviceUserRelation selectByIpAddress(String clientAddress) {
        SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
        relation.setIpAddress(clientAddress);
        Optional<SmartDeviceUserRelation> optional=smartDeviceUserRelationRepository.findOne(Example.of(relation));
        SmartDeviceUserRelation deviceUserRelation= null;
        if(!optional.isEmpty()){
             deviceUserRelation=optional.get();
        }
        return deviceUserRelation;
    }
}
