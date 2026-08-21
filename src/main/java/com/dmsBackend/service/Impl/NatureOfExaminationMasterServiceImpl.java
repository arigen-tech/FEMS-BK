// NatureOfExaminationMasterServiceImpl.java
package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.NatureOfExaminationMaster;
import com.dmsBackend.repository.NatureOfExaminationMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("natureOfExaminationMasterService")
public class NatureOfExaminationMasterServiceImpl extends GenericMasterServiceImpl<NatureOfExaminationMaster> {

    @Autowired
    private NatureOfExaminationMasterRepository repository;

    @Override
    protected MasterRepository<NatureOfExaminationMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "NatureOfExamination";
    }

    @Override
    protected NatureOfExaminationMaster newInstance() {
        return new NatureOfExaminationMaster();
    }
}
