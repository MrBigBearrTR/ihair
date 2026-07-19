package com.bigbear.ihair.entity;

import com.bigbear.ihair.common.BaseEntity;
import com.bigbear.ihair.entity.enums.BrandAssetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "brand_assets")
public class BrandAsset extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BrandAssetType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    @Column(name = "content_length", nullable = false)
    private long contentLength;
    @Column(nullable = false, length = 64)
    private String checksum;
    @Column(nullable = false)
    private int width;
    @Column(nullable = false)
    private int height;
}
