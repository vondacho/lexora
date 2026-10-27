package io.lexora.infrastructure.persistence.folder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invo_folders_status")
public class FolderStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fold_stat_id")
    Integer id;

    @Column(name = "fold_stat_name", length = 80)
    String name;
}
