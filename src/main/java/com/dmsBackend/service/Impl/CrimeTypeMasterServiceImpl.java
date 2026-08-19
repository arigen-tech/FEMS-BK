package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.CrimeTypeMaster;
import com.dmsBackend.repository.CrimeTypeMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("crimeTypeMasterService")
public class CrimeTypeMasterServiceImpl extends GenericMasterServiceImpl<CrimeTypeMaster> {

    @Autowired
    private CrimeTypeMasterRepository repository;

    @Override
    protected MasterRepository<CrimeTypeMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "CrimeType";
    }

    @Override
    protected CrimeTypeMaster newInstance() {
        return new CrimeTypeMaster();
    }
}
