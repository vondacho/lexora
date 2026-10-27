package ch.conciliations.mabecoju.infrastructure.persistence.folder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FolderJpaRepository extends JpaRepository<FolderEntity, Integer> {

    @Query("""
            select f from FolderEntity f
            where (:type is null or f.type = :type)
            order by f.name""")
    List<FolderEntity> findAllOfType(@Param("type") Integer type);

    @Query("""
            select f from FolderEntity f
            where (:type is null or f.type = :type) and lower(f.name) like lower(concat(:prefix, '%'))
            order by f.name""")
    List<FolderEntity> findByNamePrefix(@Param("prefix") String prefix, @Param("type") Integer type);

    @Query("""
            select f from FolderEntity f
            where (:type is null or f.type = :type) and cast(f.number as string) like concat(:prefix, '%')
            order by f.name""")
    List<FolderEntity> findByNumberPrefix(@Param("prefix") String prefix, @Param("type") Integer type);

    @Query("""
            select distinct f from FolderEntity f, CustomerEntity c
            where c.folderId = f.id
              and (:type is null or f.type = :type)
              and (lower(c.firstName) like lower(concat(:prefix, '%')) or lower(c.lastName) like lower(concat(:prefix, '%')))
            order by f.name""")
    List<FolderEntity> findByCustomerNamePrefix(@Param("prefix") String prefix, @Param("type") Integer type);

    List<FolderEntity> findByHandledByInOrderByName(Collection<String> handlers);
}
