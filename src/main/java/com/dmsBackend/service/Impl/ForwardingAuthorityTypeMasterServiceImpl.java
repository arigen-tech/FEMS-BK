package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.ForwardingAuthorityTypeMaster;
import com.dmsBackend.repository.ForwardingAuthorityTypeMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("forwardingAuthorityTypeMasterService")
public class ForwardingAuthorityTypeMasterServiceImpl extends GenericMasterServiceImpl<ForwardingAuthorityTypeMaster> {

    @Autowired
    private ForwardingAuthorityTypeMasterRepository repository;

    @Override
    protected MasterRepository<ForwardingAuthorityTypeMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "ForwardingAuthorityType";
    }

    @Override
    protected ForwardingAuthorityTypeMaster newInstance() {
        return new ForwardingAuthorityTypeMaster();
    }


}
