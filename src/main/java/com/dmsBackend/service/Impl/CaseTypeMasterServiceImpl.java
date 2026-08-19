package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.CaseTypeMaster;
import com.dmsBackend.repository.CaseTypeMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("caseTypeMasterService")
public class CaseTypeMasterServiceImpl extends GenericMasterServiceImpl<CaseTypeMaster> {

    @Autowired
    private CaseTypeMasterRepository repository;

    @Override
    protected MasterRepository<CaseTypeMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "CaseType";
    }

    @Override
    protected CaseTypeMaster newInstance() {
        return new CaseTypeMaster();
    }
}
