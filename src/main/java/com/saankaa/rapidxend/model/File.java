package com.saankaa.rapidxend.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Document
@Data
public class File {

    @Id
    private String id;

    @Field
    private String filename;

    @Field
    private String mimetype;

    @Field
    private Double size;   // in MB

    @Getter
    @Setter
    @Field
    private String MD5ChecksumHex;

    @Field
    private Integer lastDataBlockNumber = -1;

    @DBRef
    private List<DataBlock> dataBlocks;

    @Field
    private Integer dataBlockSize = 5; // In Mio

    @Field
    private Date createdDate = new Date();

}
