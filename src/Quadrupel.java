public class Quadrupel {
	public Processor p;
	public long address;
	public int busAction;
	public int action;

	public Quadrupel(Processor p, long address, int busAction, int action) {
		this.p = p;
		this.address = address;
		this.busAction = busAction;
		this.action = action;
	}
}
