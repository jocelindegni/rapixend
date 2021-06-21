package com.saankaa.rapidxend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Document
@Data
@NoArgsConstructor
public class Device {

    @Id
    private String id;

    @Field
    @Indexed(unique = true)
    private String name;

    @Field
    private String secretKey;  // For generating temporary password (otp)

    /**
     * Device info
     **/


    @Field
    private String model;

    @Field
    private String brand;

    @JsonIgnore
    @Field
    private Binary photo;

    @Field
    private Date createdDate = new Date();


    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", model='" + model + '\'' +
                ", brand='" + brand + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}
