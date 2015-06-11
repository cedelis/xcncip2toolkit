//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-b10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.06.11 at 02:59:27 PM CEST 
//


package org.extensiblecatalog.ncip.v2.binding.ilsdiv1_1.jaxb.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.niso.org/2008/ncip}UserPrivilegeStatusType"/>
 *         &lt;element ref="{http://www.niso.org/2008/ncip}DateOfUserPrivilegeStatus" minOccurs="0"/>
 *         &lt;element ref="{http://www.niso.org/2008/ncip}Ext" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userPrivilegeStatusType",
    "dateOfUserPrivilegeStatus",
    "ext"
})
@XmlRootElement(name = "UserPrivilegeStatus")
public class UserPrivilegeStatus {

    @XmlElement(name = "UserPrivilegeStatusType", required = true)
    protected SchemeValuePair userPrivilegeStatusType;
    @XmlElement(name = "DateOfUserPrivilegeStatus", type = String.class)
    @XmlJavaTypeAdapter(Adapter1 .class)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar dateOfUserPrivilegeStatus;
    @XmlElement(name = "Ext")
    protected Ext ext;

    /**
     * Gets the value of the userPrivilegeStatusType property.
     * 
     * @return
     *     possible object is
     *     {@link SchemeValuePair }
     *     
     */
    public SchemeValuePair getUserPrivilegeStatusType() {
        return userPrivilegeStatusType;
    }

    /**
     * Sets the value of the userPrivilegeStatusType property.
     * 
     * @param value
     *     allowed object is
     *     {@link SchemeValuePair }
     *     
     */
    public void setUserPrivilegeStatusType(SchemeValuePair value) {
        this.userPrivilegeStatusType = value;
    }

    /**
     * Gets the value of the dateOfUserPrivilegeStatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public XMLGregorianCalendar getDateOfUserPrivilegeStatus() {
        return dateOfUserPrivilegeStatus;
    }

    /**
     * Sets the value of the dateOfUserPrivilegeStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDateOfUserPrivilegeStatus(XMLGregorianCalendar value) {
        this.dateOfUserPrivilegeStatus = value;
    }

    /**
     * Gets the value of the ext property.
     * 
     * @return
     *     possible object is
     *     {@link Ext }
     *     
     */
    public Ext getExt() {
        return ext;
    }

    /**
     * Sets the value of the ext property.
     * 
     * @param value
     *     allowed object is
     *     {@link Ext }
     *     
     */
    public void setExt(Ext value) {
        this.ext = value;
    }

}