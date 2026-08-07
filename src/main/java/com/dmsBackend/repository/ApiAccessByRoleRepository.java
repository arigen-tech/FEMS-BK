package com.dmsBackend.repository;

import com.dmsBackend.entity.ApiAccessByRole;
import com.dmsBackend.entity.ApiEndpoint;
import com.dmsBackend.entity.RoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiAccessByRoleRepository extends JpaRepository<ApiAccessByRole, Integer> {

//    @Query("""
//    SELECT aar FROM ApiAccessByRole aar
//    JOIN aar.api api
//    WHERE aar.role.id = :roleId
//      AND api.endpoint = :endpoint
//      AND api.method = :method
//""")
//    Optional<ApiAccessByRole> findApiAccess(
//            Integer roleId,
//            String endpoint,
//            String method
//    );

    @Query("""
        SELECT aar
        FROM ApiAccessByRole aar
        JOIN aar.api api
        WHERE aar.role.id = :roleId
        AND api.method = :method
    """)
    List<ApiAccessByRole> findByRoleAndMethod(
            @Param("roleId") Integer roleId,
            @Param("method") String method
    );


    List<ApiAccessByRole> findByRole_Id(Integer roleId);

    Optional<ApiAccessByRole> findByRole_IdAndApi_Id(
            Integer roleId,
            Integer apiId
    );

    boolean existsByRoleAndApi(RoleMaster role, ApiEndpoint api);



    @Query("""
SELECT aar
FROM ApiAccessByRole aar
JOIN aar.api api
WHERE aar.role.id = :roleId
AND (
     api.method = :method
     OR api.method LIKE %:method%
)
""")
    List<ApiAccessByRole> findByRoleAndMethodFlexible(
            @Param("roleId") Integer roleId,
            @Param("method") String method
    );

    Optional<ApiAccessByRole> findByRoleIdAndApiId(Integer roleId, Integer apiId);

}
