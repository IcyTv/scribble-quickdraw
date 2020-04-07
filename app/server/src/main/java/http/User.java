package http;

public class User {

	public String name;
	public String pwHash;
	public String[] perms;

	public User(String name, String pwHash, String[] perms) {
		this.name = name;
		this.pwHash = pwHash;
		this.perms = perms;
	}

	public User() {
		
	}

}