package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.DistrictMaster;
import com.dmsBackend.repository.DistrictMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("districtMasterService")
public class DistrictMasterServiceImpl extends GenericMasterServiceImpl<DistrictMaster> {

    @Autowired
    private DistrictMasterRepository repository;

    @Override
    protected MasterRepository<DistrictMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "District";
    }

    @Override
    protected DistrictMaster newInstance() {
        return new DistrictMaster();
    }
}
