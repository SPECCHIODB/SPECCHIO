package ch.specchio.gui;

import java.util.EventObject;

public class DataVisEvent extends EventObject {

    private String name;
    private String occupation;
    private int ageCategory;
    private String empCat;
    private String taxId;
    private boolean usCitizen;
    private String gender;


    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public DataVisEvent(Object source) {
        super(source);
    }

    public DataVisEvent(Object source, String name, String occupation){
        super (source);
        this.name = name;
        this.occupation = occupation;
    }

    public DataVisEvent(Object source, String name, String occupation, int ageCategory){
        super (source);
        this.name = name;
        this.occupation = occupation;
        this.ageCategory = ageCategory;
    }

    public DataVisEvent(Object source, String name, String occupation, int ageCategory, String empCat){
        super (source);
        this.name = name;
        this.occupation = occupation;
        this.ageCategory = ageCategory;
        this.empCat = empCat;
    }

    public DataVisEvent(Object source, String name, String occupation, int ageCategory, String empCat, String taxId, boolean usCitizen){
        super (source);
        this.name = name;
        this.occupation = occupation;
        this.ageCategory = ageCategory;
        this.empCat = empCat;
        this.taxId = taxId;
        this.usCitizen = usCitizen;
    }

    public DataVisEvent(Object source, String name, String occupation, int ageCategory, String empCat, String taxId, boolean usCitizen, String gender){
        super (source);
        this.name = name;
        this.occupation = occupation;
        this.ageCategory = ageCategory;
        this.empCat = empCat;
        this.taxId = taxId;
        this.usCitizen = usCitizen;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public int getAgeCategory() {
        return ageCategory;
    }

    public void setAgeCategory(int ageCategory) {
        this.ageCategory = ageCategory;
    }

    public String getEmpCat() {
        return empCat;
    }

    public void setEmpCat(String empCat) {
        this.empCat = empCat;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public boolean isUsCitizen() {
        return usCitizen;
    }

    public void setUsCitizen(boolean usCitizen) {
        this.usCitizen = usCitizen;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
