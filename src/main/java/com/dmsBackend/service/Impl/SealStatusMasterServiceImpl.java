// SealStatusMasterServiceImpl.java
package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.SealStatusMaster;
import com.dmsBackend.repository.SealStatusMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("sealStatusMasterService")
public class SealStatusMasterServiceImpl extends GenericMasterServiceImpl<SealStatusMaster> {

    @Autowired
    private SealStatusMasterRepository repository;

    @Override
    protected MasterRepository<SealStatusMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "SealStatus";
    }

    @Override
    protected SealStatusMaster newInstance() {
        return new SealStatusMaster();
    }
}
