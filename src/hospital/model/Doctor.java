package hospital.model;

import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;

public class Doctor extends Person {
	private SimpleStringProperty id = new SimpleStringProperty(), speciality = new SimpleStringProperty();

	public Doctor() {

	}

	public Doctor(String id, String name, int age, GENDER gender, String speciality, String contact, String address) {
		super(name, age, gender, contact, address);
		this.id.set(id);
		this.speciality.set(speciality);
	}

	public String getId() {
		return id.get();
	}

	public void setId(String id) {
		this.id.set(id);
	}

	public SimpleStringProperty idProperty() {
		return id;
	}

	public String getSpeciality() {
		return speciality.get();
	}

	public void setSpeciality(String speciality) {
		this.speciality.set(speciality);
	}

	public SimpleStringProperty specialityProperty() {
		return speciality;
	}

	@Override
	public String toString() {
		return "Doctor [getId()=" + getId() + ", getSpeciality()=" + getSpeciality() + ", getName()=" + getName()
				+ ", getAge()=" + getAge() + ", getAddress()=" + getAddress() + ", getContact()=" + getContact() + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Doctor)) {
			return false;
		}
		Doctor doctor = (Doctor) o;
		return Objects.equals(id.getValue(), doctor.id.getValue());
	}

}
