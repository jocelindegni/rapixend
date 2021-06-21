package com.saankaa.rapidxend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Document
public class Peer {

    @Getter
    @Setter
    @Id
    private String id;

    @Getter
    @Setter
    @DBRef
    @Indexed(unique = true)
    private Device requesterDevice;

    @Getter
    @Setter
    @DBRef
    @Indexed(unique = true)
    private Device applicantDevice;

    @Getter
    @Setter
    @Field
    private boolean accepted = false;

    @Getter
    @Setter
    @Field
    private Date createdDate = new Date();
}
