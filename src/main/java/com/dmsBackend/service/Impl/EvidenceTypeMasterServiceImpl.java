package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.EvidenceTypeMaster;
import com.dmsBackend.repository.EvidenceTypeMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("evidenceTypeMasterService")
public class EvidenceTypeMasterServiceImpl extends GenericMasterServiceImpl<EvidenceTypeMaster> {

    @Autowired
    private EvidenceTypeMasterRepository repository;

    @Override
    protected MasterRepository<EvidenceTypeMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "EvidenceType";
    }

    @Override
    protected EvidenceTypeMaster newInstance() {
        return new EvidenceTypeMaster();
    }
}
