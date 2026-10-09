package com.kku.queuenotify.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "menu_item_image")
@Getter
@Setter
@NoArgsConstructor
public class MenuImage {
  @Id
  @Column(name = "menu_item_id")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "menu_item_id", nullable = false)
  private MenuItem menuItem;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "image_data", nullable = false, columnDefinition = "bytea")
  private byte[] data;

  @Column(name = "content_type", nullable = false, length = 32)
  private String contentType;
}
