package com.saankaa.rapidxend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Document
public class Transfer {

    @Getter
    @Setter
    @Field
    TransferState state;
    @Id
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    @DBRef
    private File file;
    @Getter
    @Setter
    @DBRef
    private Device sender;
    @Getter
    @Setter
    @DBRef
    private List<Device> receivers;
    @Getter
    @Setter
    @Field
    private Date createdDate = new Date();

}
