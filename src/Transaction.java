public class Transaction {
	public static enum Type {
		NULL, BusRd, BusRdX, BusUpd, BusUpgr, WB
	}
	
	private Type type;
	private int id;
	private int address;
	private boolean shared;
	
	public Transaction(Type type, int id, int address) {
		this.type = type;
		this.id = id;
		this.address = address;
		this.shared = false;
	}

	public Type getType() {
		return type;
	}

	public int getId() {
		return id;
	}

	public int getAddress() {
		return address;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}
}
