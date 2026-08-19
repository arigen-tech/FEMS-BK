package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.CityMaster;
import com.dmsBackend.repository.CityMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("cityMasterService")
public class CityMasterServiceImpl extends GenericMasterServiceImpl<CityMaster> {

    @Autowired
    private CityMasterRepository repository;

    @Override
    protected MasterRepository<CityMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "City";
    }

    @Override
    protected CityMaster newInstance() {
        return new CityMaster();
    }
}
