package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.entity.TypeMaster;
import com.dmsBackend.entity.YearMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.TypeMasterRepository;
import com.dmsBackend.service.TypeMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TypeMasterServiceImpl implements TypeMasterService {
    @Autowired
    TypeMasterRepository typeMasterRepository;
    @Override
    public TypeMaster saveTypeMaster(TypeMaster typeMaster) {
        typeMaster.setCreatedOn(Helper.getCurrentTimeStamp());
        typeMaster.setUpdatedOn(Helper.getCurrentTimeStamp());
        return typeMasterRepository.save(typeMaster);
    }

    @Override
    public TypeMaster updateTypeMaster(TypeMaster typeMaster, Integer id) {
        Optional<TypeMaster> typeMasterOptional = typeMasterRepository.findById(id);

        if (typeMasterOptional.isPresent()) {
            TypeMaster typeMaster1 = typeMasterOptional.get();
            typeMaster1.setName(typeMaster.getName());
            typeMaster1.setUpdatedOn(Helper.getCurrentTimeStamp());

            return typeMasterRepository.save(typeMaster1);
        } else {
            throw new ResourceNotFoundException("TypeMaster not found for ", "Id", id);
        }
    }

    @Override
    public void deleteByIdTypeMaster(Integer id) {

        this.typeMasterRepository.deleteById(id);
    }

    @Override
    public List<TypeMaster> findAllTypeMaster() {
        return this.typeMasterRepository.findAll();
    }

    @Override
    public Optional<TypeMaster> findTypeMasterById(Integer id) {
        return typeMasterRepository.findById(id);
    }

    @Override
    public TypeMaster findByIdType(Integer id) {
        return typeMasterRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Type not found","Id",id));
    }

    @Override
    public TypeMaster updateStatus(Integer id, Integer isActive) {
        TypeMaster typeMaster = typeMasterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type", "id", id));

        typeMaster.setUpdatedOn(Helper.getCurrentTimeStamp());
        typeMaster.setIsActive(isActive);
        return typeMasterRepository.save(typeMaster);
    }

}
