package com.saankaa.rapidxend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Document
@Data
@NoArgsConstructor
public class DataBlock {

    @Id
    private String id;

    @Field
    private Integer number;

    @Field
    private byte[] data;

    @Field
    private Date createdDate = new Date();
}
