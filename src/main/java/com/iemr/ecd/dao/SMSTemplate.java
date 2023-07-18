package com.iemr.ecd.dao;

import java.sql.Timestamp;

import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "m_smstemplate")
@Entity
public class SMSTemplate {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "SMSTemplateID", insertable = false)
	private Integer smsTemplateId;
	
	@Column(name = "SMSTemplateName")
	private String smsTemplateName;
	
	@Column(name = "SMSTemplate")
	private String smsTemplate;
	
	@Column(name = "SMSSenderName")
	private String smsSenderName;
	
	@Column(name = "SMSTypeID")
	private Integer smsTypeId;
	
	@Column(name = "ProviderServiceMapID")
	private Integer psmId;
	
	@Column(name = "Deleted", insertable = false)
	private Boolean deleted;
	
	@Column(name = "CreatedBy")
	private String createdBy;
	
	@Column(name = "CreatedDate", insertable = false, updatable = false)
	private Timestamp createdDate;
	
	@Column(name = "ModifiedBy")
	private String modifiedBy;
	
	@Column(name = "LastModDate", insertable = false, updatable = false)
	private Timestamp lastModDate;
	

}