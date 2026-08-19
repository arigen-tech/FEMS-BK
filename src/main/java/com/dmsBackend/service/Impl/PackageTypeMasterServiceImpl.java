package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.PackageTypeMaster;
import com.dmsBackend.repository.PackageTypeMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("packageTypeMasterService")
public class PackageTypeMasterServiceImpl extends GenericMasterServiceImpl<PackageTypeMaster> {

    @Autowired
    private PackageTypeMasterRepository repository;

    @Override
    protected MasterRepository<PackageTypeMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "PackageType";
    }

    @Override
    protected PackageTypeMaster newInstance() {
        return new PackageTypeMaster();
    }
}
