package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.StateMaster;
import com.dmsBackend.repository.StateMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("stateMasterService")
public class StateMasterServiceImpl extends GenericMasterServiceImpl<StateMaster> {

    @Autowired
    private StateMasterRepository repository;

    @Override
    protected MasterRepository<StateMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "State";
    }

    @Override
    protected StateMaster newInstance() {
        return new StateMaster();
    }
}
