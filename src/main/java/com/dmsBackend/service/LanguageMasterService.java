package com.dmsBackend.service;

import com.dmsBackend.entity.LanguageMaster;
import com.dmsBackend.response.LanguageMasterRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface LanguageMasterService {



        LanguageMaster saveLanguageMaster(LanguageMasterRequest request,
                                          HttpServletRequest httpRequest);

        LanguageMaster updateLanguageMaster(LanguageMasterRequest request,
                                            Long id,
                                            HttpServletRequest httpRequest);

        List<LanguageMaster> findAllLanguageMaster(int flag);

        LanguageMaster findLanguageMasterById(Long id);

        LanguageMaster updateStatus(Long id,
                                    Boolean isActive,
                                    HttpServletRequest request);


}
