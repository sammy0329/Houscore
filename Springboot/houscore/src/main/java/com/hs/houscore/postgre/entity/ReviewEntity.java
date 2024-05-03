package com.hs.houscore.postgre.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Date;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Data
@Builder
@Table(name = "review")
@SequenceGenerator(name="review_seq", sequenceName="review_seq", initialValue=1, allocationSize=1)
public class ReviewEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue (strategy = GenerationType.SEQUENCE)
    private Long id;
    private String memberId;
    private Long buildingId;
    private String address;
    private ResidenceType residenceType;
    private Integer year;
    private ResidenceFloor residenceFloor;
    @JdbcTypeCode(SqlTypes.JSON)
    private StarRating starRating;
    private String pros;
    private String cons;
    private String maintenanceCost;
    private String images;

    public enum ResidenceType {
        VILLA, APT, OFFICETEL
    }
    public enum ResidenceFloor {
        HIGH, MEDIUM, LOW
    }

    @Data
    private static class StarRating implements Serializable {
        private Double traffic;
        private Double building;
        private Double inside;
        private Double infra;
        private Double security;
    }
}