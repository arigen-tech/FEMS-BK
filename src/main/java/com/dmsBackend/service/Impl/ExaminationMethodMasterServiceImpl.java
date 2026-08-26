package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.ExaminationMethodMaster;
import com.dmsBackend.repository.ExaminationMethodMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("examinationMethodMasterService")
public class ExaminationMethodMasterServiceImpl extends GenericMasterServiceImpl<ExaminationMethodMaster> {

    @Autowired
    private ExaminationMethodMasterRepository repository;

    @Override
    protected MasterRepository<ExaminationMethodMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "ExaminationMethod";
    }

    @Override
    protected ExaminationMethodMaster newInstance() {
        return new ExaminationMethodMaster();
    }
}
