package ch.specchio.types;

import javax.xml.bind.annotation.*;

/**
 * A SPECCHIO user.
 */
@XmlRootElement(name="user")
public class User {
	
	/** user id */
	private int userId;
	
	/** username */
	private String username;
	
	/** password */
	private String password;
	
	/** first name */
	private String firstName;
	
	/** last name */
	private String lastName;
	
	/** title */
	private String title;
	
	/** e-mail address */
	private String email;
	
	/** WWW address */
	private String www;
	
	/** description */
	private String description;
	
	/** institute */
	private Institute inst;
	
	/** role */
	private String role;

	/** salt */
	private String salt;

	/** passhash */
	private String passhash;
	
	/** external (non-SPECCHIO) identifier */
	private String externalId;
	
	/** number of spectra loaded: statistics information, only filled on special request */
	private Integer number_of_spectra_loaded;
	
	/** number of campaigns: statistics information, only filled on special request */
	private Integer number_of_campaigns;
	
	/** default constructor */
	public User() {}
	
	/** constructor */
	public User(String username)
	{
		this.username = username;
	}
	
	/** copy constructor */
	public User(User other) {
		this.userId = other.userId;
		this.username = other.username;
		this.password = other.password;
		this.firstName = other.firstName;
		this.lastName = other.lastName;
		this.title = other.title;
		this.email = other.email;
		this.www = other.www;
		this.role = other.role;
		this.inst = new Institute(other.inst);
		this.externalId = other.externalId;
		this.description = other.description;
		this.salt = other.salt;
		this.passhash = other.passhash;
	}
	
	
	@XmlElement(name="description")
	public String getDescription() { return this.description; }
	public void setDescription(String description) { this.description = description; }
	
	@XmlElement(name="email")
	public String getEmailAddress() { return this.email; }
	public void setEmailAddress(String email) { this.email = email; }
	
	@XmlElement(name="external_id")
	public String getExternalId() { return this.externalId; }
	public void setExternalId(String externalId) { this.externalId = externalId; }
	
	@XmlElement(name="first_name")
	public String getFirstName() { return this.firstName; }
	public void setFirstName(String firstName) { this.firstName = firstName; }
	
	@XmlElement(name="institute")
	public Institute getInstitute() { return this.inst; }
	public void setInstitute(Institute inst) { this.inst = inst; }
	
	@XmlElement(name="last_name")
	public String getLastName() { return this.lastName; }
	public void setLastName(String lastName) { this.lastName = lastName; }
	
	@XmlElement(name="password")
	public String getPassword() { return this.password; }
	public void setPassword(String password) { this.password = password; }
	
	@XmlElement(name="role")
	public String getRole() { return this.role; }
	public void setRole(String role) { this.role = role; }
	public boolean isInRole(String role) { return this.role != null && this.role.equals(role); }
	
	@XmlElement(name="title")
	public String getTitle() { return this.title; }
	public void setTitle(String title) { this.title = title; }
	
	@XmlElement(name="user_id")
	public int getUserId() { return this.userId; }
	public void setUserId(int userId) { this.userId = userId; }
	
	@XmlElement(name="username")
	public String getUsername() { return this.username; }
	public void setUsername(String username) { this.username = username; }
	
	@XmlElement(name="www")
	public String getWwwAddress() { return this.www; }
	public void setWwwAddress(String www) { this.www = www; }

	@XmlElement(name="salt")
	public String getSalt() { return this.salt; }
	public void setSalt(String salt) { this.salt = salt; }

	@XmlElement(name="passhash")
	public String getPassHash() { return this.passhash; }
	public void setPassHash(String passhash) { this.passhash = passhash; }
	
	/** test whether or not two user objects are equal, as judged by their user ids or usernames */
	public boolean equals(Object other) {
		
		if (other instanceof User) {
			if (this.userId != 0 && ((User)other).getUserId() == this.userId) {
				return true;
			} else {
				return  ((User)other).getUsername().equals(this.username);
			}
		} else {
			return false;
		}
		
	}
	
	/** get a string containing the user's first name and last name */
	public String toString() {
		
		return this.firstName + " " + this.lastName;
		
	}

	public Integer getNumber_of_spectra_loaded() {
		return number_of_spectra_loaded;
	}

	public void setNumber_of_spectra_loaded(Integer number_of_spectra_loaded) {
		this.number_of_spectra_loaded = number_of_spectra_loaded;
	}

	public Integer getNumber_of_campaigns() {
		return number_of_campaigns;
	}

	public void setNumber_of_campaigns(Integer number_of_campaigns) {
		this.number_of_campaigns = number_of_campaigns;
	}

}
