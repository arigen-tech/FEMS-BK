package com.dmsBackend.service.Impl;


import com.dmsBackend.entity.PurposeMaster;
import com.dmsBackend.repository.PurposeMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("purposeMasterService")
public class PurposeMasterServiceImpl extends GenericMasterServiceImpl<PurposeMaster> {

    @Autowired
    private PurposeMasterRepository repository;

    @Override
    protected MasterRepository<PurposeMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "Purpose";
    }

    @Override
    protected PurposeMaster newInstance() {
        return new PurposeMaster();
    }
}
