package ch.conciliations.mabecoju.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrantJpaRepository extends JpaRepository<GrantEntity, Integer> {

    List<GrantEntity> findByUsername(String username);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from GrantEntity g where g.username = :username and g.module = :module and g.page = :page")
    void deleteGrant(@Param("username") String username, @Param("module") String module, @Param("page") String page);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from GrantEntity g where g.username = :username")
    void deleteAllOf(@Param("username") String username);
}
