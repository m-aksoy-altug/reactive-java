package org.reactive.java.iteratorpattern;

import java.util.List;

public class ConcreteIterator implements org.reactive.java.iteratorpattern.Iterator {
	private List<String> items;
	private int index = 0;
	
	public ConcreteIterator(List<String> items) {
        this.items = items;
    }
	
	@Override
	public boolean hasNext() {
		return index < items.size();
	}

	@Override
    public String next() {
        if (hasNext()) {
            return items.get(index++);
        }
        throw new IllegalStateException("No more elements");
    }
	

}
