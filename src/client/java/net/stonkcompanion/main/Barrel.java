package net.stonkcompanion.main;

public class Barrel {

	public String label = "";
	public String coords = "";
	public String ask_price = "";
	public String bid_price = "";
	public double compressed_ask_price = 0.0;
	public double compressed_bid_price = 0.0;
	public int currency_type = -1;

	public Barrel (String label, String coords, String ask_price, String bid_price, double compressed_ask_price, double compressed_bid_price, int currency_type) {
		this.label = label;
		this.coords = coords;
		this.ask_price = ask_price;
		this.bid_price = bid_price;
		this.compressed_ask_price = compressed_ask_price;
		this.compressed_bid_price = compressed_bid_price;
		this.currency_type = currency_type;
	}
	
}
