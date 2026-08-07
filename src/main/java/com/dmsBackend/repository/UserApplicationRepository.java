package com.dmsBackend.repository;

import com.dmsBackend.entity.UserApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserApplicationRepository extends JpaRepository<UserApplication,Long> {

    public List<UserApplication> findByStatusIgnoreCase(String status);
    public List<UserApplication> findByStatusInIgnoreCase(List<String> statuses);
    public List<UserApplication> findByStatusIgnoreCaseAndUrl(String status,String url);
    public List<UserApplication> findByStatusInIgnoreCaseAndUrl(List<String> statuses,String url);
    public UserApplication findByUserAppName(String userAppName);
}
