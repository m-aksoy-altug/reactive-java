package org.reactive.java.iteratorpattern;

import java.util.ArrayList;
import java.util.List;

public class ConcreteAggregate implements Aggregate {
	private List<String> items;
	
	public ConcreteAggregate() {
		this.items= new ArrayList<>();
	}
	
	@Override
	public org.reactive.java.iteratorpattern.Iterator createIterator() {
		return new ConcreteIterator(items);
	}
	
	public void addItem(String data) {
		this.items.add(data);	
	}
	
	public void get(int num) {
		this.items.get(num);	
	}
	
	public int size() {
		return this.items.size();	
	}
}
