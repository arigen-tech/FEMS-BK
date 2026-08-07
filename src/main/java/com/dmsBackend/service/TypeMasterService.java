package com.dmsBackend.service;

import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.entity.TypeMaster;
import com.dmsBackend.entity.YearMaster;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface TypeMasterService {
    TypeMaster saveTypeMaster(TypeMaster typeMaster);
    TypeMaster updateTypeMaster(TypeMaster typeMaster,Integer id);
    void deleteByIdTypeMaster(Integer id);
    List<TypeMaster> findAllTypeMaster();
    Optional<TypeMaster> findTypeMasterById(Integer id);
    TypeMaster findByIdType(Integer id);
    TypeMaster updateStatus(Integer id, Integer isActive);
}
